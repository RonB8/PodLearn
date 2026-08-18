package com.example.podlingo.testutil

import com.example.podlingo.data.local.dao.EpisodeDao
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
}
