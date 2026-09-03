package com.example.podlingo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.podlingo.data.local.entity.PodcastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(podcast: PodcastEntity)

    // Plain @Update (UPDATE ... WHERE id = :id) rather than an OnConflictStrategy.REPLACE insert -
    // REPLACE does a delete+insert of the conflicting row, which would CASCADE-delete every one of
    // this podcast's episodes (downloads, transcripts, play history) on every RSS refresh.
    @Update
    suspend fun update(podcast: PodcastEntity)

    @Query("SELECT * FROM podcasts ORDER BY title ASC")
    fun getAll(): Flow<List<PodcastEntity>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getById(id: String): PodcastEntity?

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl")
    suspend fun getByFeedUrl(feedUrl: String): PodcastEntity?
}
