package com.example.podlingo.data.remote.whisper

import com.example.podlingo.data.local.Mp3ChunkSplitter
import java.io.File
import java.io.IOException
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Transcribes an mp3 via Whisper, transparently splitting it into chunks on MPEG frame
 * boundaries when it's too large for a single request (OpenAI caps uploads at 25MB). Each
 * chunk's word/segment timestamps are offset by its position in the original file before
 * merging, so the result reads as one continuous transcript.
 */
class WhisperChunkedTranscriber @Inject constructor(
    private val whisperApi: WhisperApi,
    private val mp3ChunkSplitter: Mp3ChunkSplitter,
) {

    suspend fun transcribe(
        file: File,
        maxRequestBytes: Long = MAX_REQUEST_BYTES,
        onChunkProgress: suspend (chunkIndex: Int, chunkCount: Int) -> Unit,
    ): WhisperTranscriptionResponse {
        if (file.length() <= maxRequestBytes) {
            onChunkProgress(1, 1)
            return transcribeOne(file)
        }

        val chunks = mp3ChunkSplitter.split(file, maxRequestBytes)
        if (chunks.size <= 1) {
            val sizeMb = file.length() / 1_000_000
            throw IOException(
                "Episode audio is ${sizeMb}MB, over the Whisper API's per-request limit, and " +
                    "its MP3 frame structure couldn't be parsed for splitting.",
            )
        }

        try {
            val segments = mutableListOf<WhisperSegment>()
            val words = mutableListOf<WhisperWord>()
            val text = StringBuilder()
            chunks.forEachIndexed { index, chunk ->
                onChunkProgress(index + 1, chunks.size)
                val response = transcribeOne(chunk.file)
                segments += response.segments.map {
                    it.copy(start = it.start + chunk.startTimeSec, end = it.end + chunk.startTimeSec)
                }
                words += response.words.map {
                    it.copy(start = it.start + chunk.startTimeSec, end = it.end + chunk.startTimeSec)
                }
                text.append(response.text)
            }
            return WhisperTranscriptionResponse(text = text.toString(), segments = segments, words = words)
        } finally {
            chunks.forEach { if (it.file != file) it.file.delete() }
        }
    }

    private suspend fun transcribeOne(file: File): WhisperTranscriptionResponse {
        val requestFile = file.asRequestBody("audio/mpeg".toMediaType())
        val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val modelPart = "whisper-1".toRequestBody("text/plain".toMediaType())
        val responseFormatPart = "verbose_json".toRequestBody("text/plain".toMediaType())
        val wordGranularityPart = "word".toRequestBody("text/plain".toMediaType())
        val segmentGranularityPart = "segment".toRequestBody("text/plain".toMediaType())
        // Without this, Whisper auto-detects the spoken language per request - which for a long
        // episode split into chunks can misfire on any chunk that's short on clear speech (music,
        // a sponsor read, an intro), transcribing it in the wrong language entirely. This app is
        // specifically for English learners, so the source language is never actually ambiguous.
        val languagePart = "en".toRequestBody("text/plain".toMediaType())
        return whisperApi.transcribe(
            filePart,
            modelPart,
            responseFormatPart,
            wordGranularityPart,
            segmentGranularityPart,
            languagePart,
        )
    }

    companion object {
        /** OpenAI's Whisper endpoint caps uploads at 25MB; kept a little under that for headroom. */
        const val MAX_REQUEST_BYTES = 24_000_000L
    }
}
