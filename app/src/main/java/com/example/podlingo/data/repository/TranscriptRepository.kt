package com.example.podlingo.data.repository

import android.content.Context
import com.example.podlingo.data.local.dao.EpisodeDao
import com.example.podlingo.data.local.dao.TranscriptDao
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.SentenceEntity
import com.example.podlingo.data.local.entity.TranscriptStatus
import com.example.podlingo.data.local.entity.WordEntity
import com.example.podlingo.data.remote.FileDownloader
import com.example.podlingo.data.remote.whisper.WhisperChunkedTranscriber
import com.example.podlingo.data.remote.whisper.WhisperErrorResponse
import com.example.podlingo.data.remote.whisper.WhisperTranscriptionResponse
import com.example.podlingo.core.WordTiming
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * Orchestrates the one-time-per-episode preprocessing pipeline (spec section 2): download the
 * mp3, run it through Whisper for word-level timestamps, group words into sentences using
 * Whisper's own segment boundaries, and cache the result in Room so re-listening never re-runs
 * STT. Progress is surfaced as a [Flow] for the UI's loading state.
 */
class TranscriptRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileDownloader: FileDownloader,
    private val chunkedTranscriber: WhisperChunkedTranscriber,
    private val episodeDao: EpisodeDao,
    private val transcriptDao: TranscriptDao,
    private val episodeStorageManager: EpisodeStorageManager,
) {

    fun preprocess(episode: EpisodeEntity): Flow<PreprocessingProgress> = channelFlow {
        val localFile = resolveLocalFile(episode)
        val alreadyTranscribed = episode.transcriptStatus == TranscriptStatus.READY
        // A ready transcript with its audio still on disk needs nothing further - the common case.
        // If the audio was evicted for storage space (see EpisodeStorageManager) but the transcript
        // is still cached, only the download below re-runs; transcription is never repeated.
        if (alreadyTranscribed && localFile.exists()) {
            send(PreprocessingProgress.Ready)
            return@channelFlow
        }

        if (!localFile.exists()) {
            // Only flip the persisted status for a genuinely new episode - re-downloading evicted
            // audio for an already-READY transcript shouldn't touch that status either way, since
            // the transcript itself was never affected.
            if (!alreadyTranscribed) episodeDao.updateTranscriptStatus(episode.id, TranscriptStatus.DOWNLOADING)
            send(PreprocessingProgress.Downloading(0f))
            val downloadResult = fileDownloader.download(episode.audioUrl, localFile) { downloaded, total ->
                val fraction = if (total > 0) downloaded.toFloat() / total else -1f
                trySend(PreprocessingProgress.Downloading(fraction))
            }
            if (downloadResult.isFailure) {
                val message = downloadResult.exceptionOrNull()?.message ?: "Download failed"
                if (alreadyTranscribed) {
                    // The transcript is still perfectly good - only the re-download failed, so
                    // don't persist FAILED over a READY transcript (that would force a pointless,
                    // costly re-transcription on the next retry).
                    send(PreprocessingProgress.Failed(message))
                } else {
                    failEpisode(episode.id, message)
                }
                return@channelFlow
            }
            episodeDao.updateLocalFilePath(episode.id, localFile.absolutePath)
            episodeStorageManager.evictIfOverLimit(protectedEpisodeId = episode.id)
        }

        if (alreadyTranscribed) {
            send(PreprocessingProgress.Ready)
            return@channelFlow
        }

        send(PreprocessingProgress.Transcribing())
        episodeDao.updateTranscriptStatus(episode.id, TranscriptStatus.TRANSCRIBING)
        val transcription = try {
            chunkedTranscriber.transcribe(localFile) { chunkIndex, chunkCount ->
                send(PreprocessingProgress.Transcribing(chunkIndex, chunkCount))
            }
        } catch (e: Exception) {
            failEpisode(episode.id, describeTranscriptionError(e))
            return@channelFlow
        }

        send(PreprocessingProgress.Processing)
        episodeDao.updateTranscriptStatus(episode.id, TranscriptStatus.PROCESSING)
        val (sentences, words) = mapToEntities(episode.id, transcription)
        transcriptDao.replaceTranscript(episode.id, sentences, words)

        episodeDao.updateTranscriptStatus(episode.id, TranscriptStatus.READY)
        send(PreprocessingProgress.Ready)
    }

    private suspend fun ProducerScope<PreprocessingProgress>.failEpisode(
        episodeId: String,
        message: String,
    ) {
        episodeDao.updateTranscriptStatus(episodeId, TranscriptStatus.FAILED, message)
        send(PreprocessingProgress.Failed(message))
    }

    suspend fun getWordTimings(episodeId: String): List<WordTiming> =
        transcriptDao.getWordsForEpisode(episodeId).map { WordTiming(it.word, it.startMs, it.endMs, it.sentenceId) }

    suspend fun getSentence(sentenceId: String): SentenceEntity? = transcriptDao.getSentence(sentenceId)

    suspend fun getSentences(episodeId: String): List<SentenceEntity> = transcriptDao.getSentencesForEpisode(episodeId)

    private fun resolveLocalFile(episode: EpisodeEntity): File {
        episode.localFilePath?.let { path ->
            val existing = File(path)
            if (existing.exists()) return existing
        }
        // Episode ids may come from RSS guids/URLs and aren't filesystem-safe, so derive a
        // stable, safe filename instead of using the id directly.
        val safeName = UUID.nameUUIDFromBytes(episode.id.toByteArray()).toString()
        return File(File(context.filesDir, "episodes"), "$safeName.mp3")
    }

    private fun describeTranscriptionError(e: Exception): String {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val parsedMessage = errorBody?.let {
                try {
                    errorJson.decodeFromString<WhisperErrorResponse>(it).error?.message
                } catch (parseError: Exception) {
                    null
                }
            }
            return parsedMessage ?: "OpenAI API error: HTTP ${e.code()}"
        }
        return e.message ?: "Transcription failed"
    }

    /**
     * Whisper's `segments` are already pause/punctuation-based chunks, so one segment = one
     * sentence row. Each word is assigned to the last segment that had started by the time the
     * word was spoken (a two-pointer sweep over both time-sorted lists).
     */
    private fun mapToEntities(
        episodeId: String,
        response: WhisperTranscriptionResponse,
    ): Pair<List<SentenceEntity>, List<WordEntity>> {
        val sortedSegments = response.segments.sortedBy { it.start }
        val sentences = sortedSegments.mapIndexed { index, segment ->
            SentenceEntity(
                id = "${episodeId}_s$index",
                episodeId = episodeId,
                startMs = (segment.start * 1000).roundToLong(),
                endMs = (segment.end * 1000).roundToLong(),
                fullText = segment.text.trim(),
                orderIndex = index,
            )
        }

        val words = mutableListOf<WordEntity>()
        var segmentCursor = 0
        response.words.sortedBy { it.start }.forEachIndexed { index, word ->
            val wordStartMs = (word.start * 1000).roundToLong()
            while (segmentCursor < sentences.lastIndex && sentences[segmentCursor + 1].startMs <= wordStartMs) {
                segmentCursor++
            }
            val owningSentenceId = sentences.getOrNull(segmentCursor)?.id ?: return@forEachIndexed
            words += WordEntity(
                episodeId = episodeId,
                sentenceId = owningSentenceId,
                word = word.word,
                startMs = wordStartMs,
                endMs = (word.end * 1000).roundToLong(),
                orderIndex = index,
            )
        }

        return sentences to words
    }

    companion object {
        private val errorJson = Json { ignoreUnknownKeys = true }
    }
}
