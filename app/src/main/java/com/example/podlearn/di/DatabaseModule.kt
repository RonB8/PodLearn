package com.example.podlearn.di

import android.content.Context
import androidx.room.Room
import com.example.podlearn.data.local.AppDatabase
import com.example.podlearn.data.local.dao.EpisodeDao
import com.example.podlearn.data.local.dao.PodcastDao
import com.example.podlearn.data.local.dao.TranscriptDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME).build()

    @Provides
    fun providePodcastDao(database: AppDatabase): PodcastDao = database.podcastDao()

    @Provides
    fun provideEpisodeDao(database: AppDatabase): EpisodeDao = database.episodeDao()

    @Provides
    fun provideTranscriptDao(database: AppDatabase): TranscriptDao = database.transcriptDao()
}
