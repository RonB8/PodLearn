package com.example.podlingo.testutil

import com.example.podlingo.data.local.dao.DownloadedEpisodeRef
import com.example.podlingo.data.local.dao.EpisodeDao
import com.example.podlingo.data.local.dao.RecentlyPlayedItem
import com.example.podlingo.data.local.dao.RecentlyPlayedPodcast
import com.example.podlingo.data.local.dao.SavedEpisodeItem
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.TranscriptStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeEpisodeDao : EpisodeDao {

    private val episodes = MutableStateFlow<List<EpisodeEntity>>(emptyList())

    override suspend fun insertAll(episodes: List<EpisodeEntity>) {
        val incomingIds = episodes.mapTo(HashSet()) { it.id }
        this.episodes.update { current -> current.filterNot { it.id in incomingIds } + episodes }
    }

    override fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>> =
        episodes.map { list -> list.filter { it.podcastId == podcastId } }

    override suspend fun getById(id: String): EpisodeEntity? = episodes.value.find { it.id == id }

    override fun getByIdFlow(id: String): Flow<EpisodeEntity?> = episodes.map { list -> list.find { it.id == id } }

    override suspend fun updateTranscriptStatus(id: String, status: TranscriptStatus, error: String?) {
        episodes.update { list -> list.map { if (it.id == id) it.copy(transcriptStatus = status, transcriptError = error) else it } }
    }

    override suspend fun updateLocalFilePath(id: String, path: String) {
        episodes.update { list -> list.map { if (it.id == id) it.copy(localFilePath = path) else it } }
    }

    override suspend fun clearLocalFilePath(id: String) {
        episodes.update { list -> list.map { if (it.id == id) it.copy(localFilePath = null) else it } }
    }

    override suspend fun getDownloadedEpisodesByLruOrder(): List<DownloadedEpisodeRef> =
        episodes.value.filter { it.localFilePath != null }
            .sortedWith(compareBy(nullsFirst()) { it.lastPlayedEpochMs })
            .map { DownloadedEpisodeRef(it.id, it.localFilePath!!) }

    override suspend fun updateLastPlayed(id: String, epochMs: Long) {
        episodes.update { list -> list.map { if (it.id == id) it.copy(lastPlayedEpochMs = epochMs) else it } }
    }

    override suspend fun clearLastPlayed(id: String) {
        episodes.update { list -> list.map { if (it.id == id) it.copy(lastPlayedEpochMs = null) else it } }
    }

    override suspend fun markStartQuizCompleted(id: String) {
        episodes.update { list -> list.map { if (it.id == id) it.copy(startQuizCompleted = true) else it } }
    }

    override fun getRecentlyPlayed(): Flow<List<RecentlyPlayedItem>> = episodes.map { list ->
        list.filter { it.lastPlayedEpochMs != null }
            .sortedByDescending { it.lastPlayedEpochMs }
            .map {
                RecentlyPlayedItem(
                    it.id,
                    it.title,
                    podcastTitle = "",
                    artworkUrl = null,
                    lastPlayedEpochMs = it.lastPlayedEpochMs!!,
                )
            }
    }

    override fun getRecentlyPlayedPodcasts(): Flow<List<RecentlyPlayedPodcast>> = episodes.map { list ->
        list.filter { it.lastPlayedEpochMs != null }
            .groupBy { it.podcastId }
            .map { (podcastId, group) ->
                RecentlyPlayedPodcast(
                    id = podcastId,
                    title = "",
                    artworkUrl = null,
                    lastPlayedEpochMs = group.maxOf { it.lastPlayedEpochMs!! },
                )
            }
            .sortedByDescending { it.lastPlayedEpochMs }
    }

    override fun getSavedEpisodes(): Flow<List<SavedEpisodeItem>> = episodes.map { list ->
        list.filter { it.localFilePath != null }
            .sortedByDescending { it.pubDateEpochMs }
            .map {
                SavedEpisodeItem(
                    id = it.id,
                    title = it.title,
                    podcastTitle = "",
                    artworkUrl = null,
                    pubDateEpochMs = it.pubDateEpochMs,
                    durationSec = it.durationSec,
                )
            }
    }
}
