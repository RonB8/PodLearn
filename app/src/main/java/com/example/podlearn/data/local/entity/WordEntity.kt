package com.example.podlearn.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SentenceEntity::class,
            parentColumns = ["id"],
            childColumns = ["sentenceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["episodeId", "startMs"]), Index("sentenceId")],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val episodeId: String,
    val sentenceId: String,
    val word: String,
    val startMs: Long,
    val endMs: Long,
    val orderIndex: Int,
)
