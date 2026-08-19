package com.example.podlingo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_episodes",
    primaryKeys = ["playlistId", "episodeId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("episodeId")],
)
data class PlaylistEpisodeCrossRef(
    val playlistId: String,
    val episodeId: String,
    val addedAtEpochMs: Long,
)
