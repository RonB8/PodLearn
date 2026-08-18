package com.example.podlingo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.TranscriptStatus
import kotlinx.coroutines.flow.Flow

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
}
