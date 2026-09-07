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

/** Adds the app-wide per-word vocabulary knowledge table (known/unknown + cached translation). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `word_knowledge` (" +
                "`word` TEXT NOT NULL, `status` TEXT NOT NULL, `hebrewTranslation` TEXT, " +
                "`updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`word`))",
        )
    }
}

/** Tracks whether an episode has already had its vocabulary calibration panel auto-offered, so re-opening it doesn't re-prompt. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE episodes ADD COLUMN vocabCalibrated INTEGER NOT NULL DEFAULT 0")
    }
}

/** Tracks whether the pre-episode "quiz yourself first" flow has been completed, so a fresh start of the same episode doesn't re-offer it once it's done. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE episodes ADD COLUMN startQuizCompleted INTEGER NOT NULL DEFAULT 0")
    }
}
