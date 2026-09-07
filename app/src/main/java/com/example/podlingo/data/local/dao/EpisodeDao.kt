package com.example.podlingo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.TranscriptStatus
import kotlinx.coroutines.flow.Flow

/** Joined projection for the recently-played list - needs the podcast title/artwork, which [EpisodeEntity] doesn't carry. */
data class RecentlyPlayedItem(
    val id: String,
    val title: String,
    val podcastTitle: String,
    val artworkUrl: String?,
    val lastPlayedEpochMs: Long,
)

/** A downloaded episode's on-disk location, for storage-usage accounting and LRU eviction (see EpisodeStorageManager). */
data class DownloadedEpisodeRef(
    val id: String,
    val localFilePath: String,
)

/** A podcast with listening history, for the Home tab's "Podcasts" view - grouped from [RecentlyPlayedItem] by podcast. */
data class RecentlyPlayedPodcast(
    val id: String,
    val title: String,
    val artworkUrl: String?,
    val lastPlayedEpochMs: Long,
)

/** Joined projection for the Library tab's "Saved" list - every downloaded episode across all podcasts. */
data class SavedEpisodeItem(
    val id: String,
    val title: String,
    val podcastTitle: String,
    val artworkUrl: String?,
    val pubDateEpochMs: Long?,
    val durationSec: Long?,
)

@Dao
interface EpisodeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY pubDateEpochMs DESC")
    fun getByPodcast(podcastId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: String): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE id = :id")
    fun getByIdFlow(id: String): Flow<EpisodeEntity?>

    @Query("UPDATE episodes SET transcriptStatus = :status, transcriptError = :error WHERE id = :id")
    suspend fun updateTranscriptStatus(id: String, status: TranscriptStatus, error: String? = null)

    @Query("UPDATE episodes SET localFilePath = :path WHERE id = :id")
    suspend fun updateLocalFilePath(id: String, path: String)

    @Query("UPDATE episodes SET localFilePath = NULL WHERE id = :id")
    suspend fun clearLocalFilePath(id: String)

    /** Downloaded episodes, least-recently-played first (never-played episodes sort first, ahead of anything with a play timestamp) - the eviction order for [com.example.podlingo.data.repository.EpisodeStorageManager]. */
    @Query("SELECT id, localFilePath FROM episodes WHERE localFilePath IS NOT NULL ORDER BY lastPlayedEpochMs ASC")
    suspend fun getDownloadedEpisodesByLruOrder(): List<DownloadedEpisodeRef>

    @Query("UPDATE episodes SET lastPlayedEpochMs = :epochMs WHERE id = :id")
    suspend fun updateLastPlayed(id: String, epochMs: Long)

    @Query("UPDATE episodes SET lastPlayedEpochMs = NULL WHERE id = :id")
    suspend fun clearLastPlayed(id: String)

    @Query("UPDATE episodes SET startQuizCompleted = 1 WHERE id = :id")
    suspend fun markStartQuizCompleted(id: String)

    @Query(
        """
        SELECT episodes.id AS id, episodes.title AS title, podcasts.title AS podcastTitle,
               podcasts.imageUrl AS artworkUrl, episodes.lastPlayedEpochMs AS lastPlayedEpochMs
        FROM episodes
        INNER JOIN podcasts ON podcasts.id = episodes.podcastId
        WHERE episodes.lastPlayedEpochMs IS NOT NULL
        ORDER BY episodes.lastPlayedEpochMs DESC
        """,
    )
    fun getRecentlyPlayed(): Flow<List<RecentlyPlayedItem>>

    /** Distinct podcasts you have listening history with, most-recently-played first. */
    @Query(
        """
        SELECT podcasts.id AS id, podcasts.title AS title, podcasts.imageUrl AS artworkUrl,
               MAX(episodes.lastPlayedEpochMs) AS lastPlayedEpochMs
        FROM episodes
        INNER JOIN podcasts ON podcasts.id = episodes.podcastId
        WHERE episodes.lastPlayedEpochMs IS NOT NULL
        GROUP BY episodes.podcastId
        ORDER BY lastPlayedEpochMs DESC
        """,
    )
    fun getRecentlyPlayedPodcasts(): Flow<List<RecentlyPlayedPodcast>>

    /** Every downloaded episode across all podcasts, newest published first - the Library tab's "Saved" list. */
    @Query(
        """
        SELECT episodes.id AS id, episodes.title AS title, podcasts.title AS podcastTitle,
               podcasts.imageUrl AS artworkUrl, episodes.pubDateEpochMs AS pubDateEpochMs,
               episodes.durationSec AS durationSec
        FROM episodes
        INNER JOIN podcasts ON podcasts.id = episodes.podcastId
        WHERE episodes.localFilePath IS NOT NULL
        ORDER BY episodes.pubDateEpochMs DESC
        """,
    )
    fun getSavedEpisodes(): Flow<List<SavedEpisodeItem>>
}
