package com.example.podlingo.testutil

import com.example.podlingo.data.local.dao.PodcastDao
import com.example.podlingo.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakePodcastDao : PodcastDao {

    private val podcasts = MutableStateFlow<List<PodcastEntity>>(emptyList())

    override suspend fun insert(podcast: PodcastEntity) {
        podcasts.update { current -> current.filterNot { it.id == podcast.id } + podcast }
    }

    override suspend fun update(podcast: PodcastEntity) {
        podcasts.update { current -> current.map { if (it.id == podcast.id) podcast else it } }
    }

    override fun getAll(): Flow<List<PodcastEntity>> = podcasts

    override suspend fun getById(id: String): PodcastEntity? = podcasts.value.find { it.id == id }

    override suspend fun getByFeedUrl(feedUrl: String): PodcastEntity? =
        podcasts.value.find { it.feedUrl == feedUrl }
}
