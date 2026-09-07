package com.example.podlingo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [
        ForeignKey(
            entity = PodcastEntity::class,
            parentColumns = ["id"],
            childColumns = ["podcastId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("podcastId")],
)
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val podcastId: String,
    val title: String,
    val audioUrl: String,
    val localFilePath: String? = null,
    val pubDateEpochMs: Long? = null,
    val durationSec: Long? = null,
    val transcriptStatus: TranscriptStatus = TranscriptStatus.NONE,
    val transcriptError: String? = null,
    val lastPlayedEpochMs: Long? = null,
    /** Whether the vocabulary calibration panel has already been offered for this episode - once true, opening it again never auto-prompts, only an explicit "Auto translate" chip tap does. */
    val vocabCalibrated: Boolean = false,
    /** Whether the pre-episode "quiz yourself on the words you don't know" flow has been completed for this episode - only true once the user finishes it, not merely declines, so a decline offers it again on the next fresh start. */
    val startQuizCompleted: Boolean = false,
)
