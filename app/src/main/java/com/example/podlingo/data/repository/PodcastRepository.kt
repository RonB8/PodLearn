package com.example.podlingo.data.repository

import com.example.podlingo.data.local.dao.EpisodeDao
import com.example.podlingo.data.local.dao.PodcastDao
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.PodcastEntity
import com.example.podlingo.data.remote.rss.RssParser
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class PodcastRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
) {

    /** Fetches and parses the RSS feed at [feedUrl], then upserts the podcast and its episodes. */
    suspend fun addPodcastByRssUrl(feedUrl: String): Result<PodcastEntity> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(feedUrl).build()
            val feed = okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Failed to fetch feed: HTTP ${response.code}"))
                }
                val body = response.body
                    ?: return@withContext Result.failure(IOException("Empty response body"))
                RssParser.parse(body.byteStream())
            }

            val existing = podcastDao.getByFeedUrl(feedUrl)
            val podcastId = existing?.id ?: UUID.randomUUID().toString()
            val podcast = PodcastEntity(
                id = podcastId,
                feedUrl = feedUrl,
                title = feed.title.ifBlank { feedUrl },
                imageUrl = feed.imageUrl,
                description = feed.description,
            )
            podcastDao.insert(podcast)

            val episodes = feed.items.mapNotNull { item ->
                val audioUrl = item.audioUrl ?: return@mapNotNull null
                EpisodeEntity(
                    id = item.guid ?: audioUrl,
                    podcastId = podcastId,
                    title = item.title.ifBlank { "Untitled episode" },
                    audioUrl = audioUrl,
                    pubDateEpochMs = item.pubDateEpochMs,
                    durationSec = item.durationSec,
                )
            }
            episodeDao.insertAll(episodes)

            Result.success(podcast)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPodcasts(): Flow<List<PodcastEntity>> = podcastDao.getAll()

    fun getEpisodes(podcastId: String): Flow<List<EpisodeEntity>> = episodeDao.getByPodcast(podcastId)

    suspend fun getEpisode(episodeId: String): EpisodeEntity? = episodeDao.getById(episodeId)
}
