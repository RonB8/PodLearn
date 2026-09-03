package com.example.podlingo.data.repository

import com.example.podlingo.data.local.dao.DownloadedEpisodeRef
import com.example.podlingo.data.local.dao.EpisodeDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks how much disk space downloaded episode audio is using and enforces
 * [SettingsRepository.storageLimitBytes] by deleting the least-recently-played episodes' audio
 * files once the limit is exceeded. Transcripts are never touched - a deleted episode's audio is
 * transparently re-downloaded the next time it's opened (see [TranscriptRepository.preprocess]),
 * so eviction never costs a re-transcription.
 */
@Singleton
class EpisodeStorageManager @Inject constructor(
    private val episodeDao: EpisodeDao,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun totalUsedBytes(): Long =
        episodeDao.getDownloadedEpisodesByLruOrder().sumOf { fileLength(it.localFilePath) }

    /** Manual counterpart to the automatic LRU eviction below - the row menu's "Delete" action on a downloaded episode. Transcript, history and playlist membership are all untouched. */
    suspend fun deleteDownload(episodeId: String) {
        val path = episodeDao.getById(episodeId)?.localFilePath ?: return
        if (File(path).delete()) {
            episodeDao.clearLocalFilePath(episodeId)
        }
    }

    /**
     * Deletes downloaded audio files, least-recently-played first, until total usage is back
     * under the configured limit. [protectedEpisodeId] (typically the episode that was just
     * downloaded and is about to play) is never a deletion candidate, even if it's the
     * least-recently-played one on record.
     */
    suspend fun evictIfOverLimit(protectedEpisodeId: String? = null) {
        val limitBytes = settingsRepository.storageLimitBytes.value
        val downloaded: List<Pair<DownloadedEpisodeRef, Long>> =
            episodeDao.getDownloadedEpisodesByLruOrder().map { it to fileLength(it.localFilePath) }
        var usedBytes = downloaded.sumOf { it.second }
        if (usedBytes <= limitBytes) return

        for ((episode, size) in downloaded) {
            if (usedBytes <= limitBytes) break
            if (episode.id == protectedEpisodeId) continue
            if (File(episode.localFilePath).delete()) {
                episodeDao.clearLocalFilePath(episode.id)
                usedBytes -= size
            }
        }
    }

    private fun fileLength(path: String): Long = File(path).let { if (it.exists()) it.length() else 0L }
}
