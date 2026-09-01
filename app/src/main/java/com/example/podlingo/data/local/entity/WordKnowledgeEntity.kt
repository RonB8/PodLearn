package com.example.podlingo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WordKnowledgeStatus {
    KNOWN,
    UNKNOWN,
}

/**
 * A user's per-word vocabulary profile, app-wide (not scoped to any one episode/podcast) - keyed
 * by [com.example.podlingo.core.WordNormalizer.normalize] so it lines up with
 * [com.example.podlingo.data.repository.WordDifficultyRepository]'s difficulty lookups. Once a
 * word has a row here it's never asked about again in the vocabulary calibration panel.
 */
@Entity(tableName = "word_knowledge")
data class WordKnowledgeEntity(
    @PrimaryKey val word: String,
    val status: WordKnowledgeStatus,
    /** Filled lazily the first time this word is actually needed (playback popup or quiz) - cached forever after that. */
    val hebrewTranslation: String? = null,
    val updatedAtEpochMs: Long,
)
