package com.example.podlearn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sentences",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("episodeId")],
)
data class SentenceEntity(
    @PrimaryKey val id: String,
    val episodeId: String,
    val startMs: Long,
    val endMs: Long,
    val fullText: String,
    val orderIndex: Int,
)
