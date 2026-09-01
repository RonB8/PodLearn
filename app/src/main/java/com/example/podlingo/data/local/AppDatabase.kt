package com.example.podlingo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.podlingo.data.local.dao.EpisodeDao
import com.example.podlingo.data.local.dao.PlaylistDao
import com.example.podlingo.data.local.dao.PodcastDao
import com.example.podlingo.data.local.dao.TranscriptDao
import com.example.podlingo.data.local.dao.WordKnowledgeDao
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.PlaylistEntity
import com.example.podlingo.data.local.entity.PlaylistEpisodeCrossRef
import com.example.podlingo.data.local.entity.PodcastEntity
import com.example.podlingo.data.local.entity.SentenceEntity
import com.example.podlingo.data.local.entity.WordEntity
import com.example.podlingo.data.local.entity.WordKnowledgeEntity

@Database(
    entities = [
        PodcastEntity::class, EpisodeEntity::class, SentenceEntity::class, WordEntity::class,
        PlaylistEntity::class, PlaylistEpisodeCrossRef::class, WordKnowledgeEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun podcastDao(): PodcastDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun transcriptDao(): TranscriptDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun wordKnowledgeDao(): WordKnowledgeDao

    companion object {
        const val DATABASE_NAME = "podlingo.db"
    }
}
