package com.example.podlingo.data.repository

import com.example.podlingo.data.local.dao.EpisodeDao
import com.example.podlingo.data.local.dao.PodcastDao
import com.example.podlingo.data.local.dao.RecentlyPlayedItem
import com.example.podlingo.data.local.dao.RecentlyPlayedPodcast
import com.example.podlingo.data.local.dao.SavedEpisodeItem
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.PodcastEntity
import com.example.podlingo.data.remote.rss.RssParser
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Request

class PodcastRepository @Inject constructor(
    private val callFactory: Call.Factory,
    private val podcastDao: PodcastDao,
    private val episodeDao: EpisodeDao,
) {

    /** Fetches and parses the RSS feed at [feedUrl], then upserts the podcast and its episodes. */
    suspend fun addPodcastByRssUrl(feedUrl: String): Result<PodcastEntity> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(feedUrl).build()
            val feed = callFactory.newCall(request).execute().use { response ->
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
            if (existing != null) podcastDao.update(podcast) else podcastDao.insert(podcast)

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

    suspend fun getPodcast(podcastId: String): PodcastEntity? = podcastDao.getById(podcastId)

    suspend fun getByFeedUrl(feedUrl: String): PodcastEntity? = podcastDao.getByFeedUrl(feedUrl)

    fun getEpisodes(podcastId: String): Flow<List<EpisodeEntity>> = episodeDao.getByPodcast(podcastId)

    suspend fun getEpisode(episodeId: String): EpisodeEntity? = episodeDao.getById(episodeId)

    /** Bumps [episodeId] to the top of the recently-played list, recording it as played now. */
    suspend fun recordEpisodePlayed(episodeId: String) {
        episodeDao.updateLastPlayed(episodeId, System.currentTimeMillis())
    }

    suspend fun markEpisodeVocabCalibrated(episodeId: String) {
        episodeDao.markVocabCalibrated(episodeId)
    }

    suspend fun markStartQuizCompleted(episodeId: String) {
        episodeDao.markStartQuizCompleted(episodeId)
    }

    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedItem>> = episodeDao.getRecentlyPlayed()

    fun getRecentlyPlayedPodcasts(): Flow<List<RecentlyPlayedPodcast>> = episodeDao.getRecentlyPlayedPodcasts()

    /** Drops [episodeId] off the recently-played list - the episode, its download, and its transcript are untouched. */
    suspend fun removeFromHistory(episodeId: String) {
        episodeDao.clearLastPlayed(episodeId)
    }

    fun getSavedEpisodes(): Flow<List<SavedEpisodeItem>> = episodeDao.getSavedEpisodes()
}
