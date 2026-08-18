package com.example.podlingo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.podlingo.data.local.dao.EpisodeDao
import com.example.podlingo.data.local.dao.PodcastDao
import com.example.podlingo.data.local.dao.TranscriptDao
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.PodcastEntity
import com.example.podlingo.data.local.entity.SentenceEntity
import com.example.podlingo.data.local.entity.WordEntity

@Database(
    entities = [PodcastEntity::class, EpisodeEntity::class, SentenceEntity::class, WordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun transcriptDao(): TranscriptDao

    companion object {
        const val DATABASE_NAME = "podlingo.db"
    }
}
