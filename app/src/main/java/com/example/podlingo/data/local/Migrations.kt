package com.example.podlingo.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the "recently played" tracking column without touching any existing downloaded/transcribed data. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE episodes ADD COLUMN lastPlayedEpochMs INTEGER")
    }
}

/** Adds the playlists + playlist-episode membership tables. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlists` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `playlist_episodes` (" +
                "`playlistId` TEXT NOT NULL, `episodeId` TEXT NOT NULL, `addedAtEpochMs` INTEGER NOT NULL, " +
                "PRIMARY KEY(`playlistId`, `episodeId`), " +
                "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON DELETE CASCADE, " +
                "FOREIGN KEY(`episodeId`) REFERENCES `episodes`(`id`) ON DELETE CASCADE)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_episodes_episodeId` ON `playlist_episodes` (`episodeId`)")
    }
}
