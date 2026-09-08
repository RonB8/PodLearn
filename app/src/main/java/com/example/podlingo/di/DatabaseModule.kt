package com.example.podlingo.di

import android.content.Context
import androidx.room.Room
import com.example.podlingo.data.local.AppDatabase
import com.example.podlingo.data.local.MIGRATION_1_2
import com.example.podlingo.data.local.MIGRATION_2_3
import com.example.podlingo.data.local.MIGRATION_3_4
import com.example.podlingo.data.local.MIGRATION_4_5
import com.example.podlingo.data.local.MIGRATION_5_6
import com.example.podlingo.data.local.MIGRATION_6_7
import com.example.podlingo.data.local.dao.EpisodeDao
import com.example.podlingo.data.local.dao.PlaylistDao
import com.example.podlingo.data.local.dao.PodcastDao
import com.example.podlingo.data.local.dao.TranscriptDao
import com.example.podlingo.data.local.dao.WordKnowledgeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .build()

    @Provides
    fun providePodcastDao(database: AppDatabase): PodcastDao = database.podcastDao()

    @Provides
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao = database.episodeDao()

    @Provides
    fun provideTranscriptDao(database: AppDatabase): TranscriptDao = database.transcriptDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideWordKnowledgeDao(database: AppDatabase): WordKnowledgeDao = database.wordKnowledgeDao()
}
