package com.example.podlearn.core

/**
 * A single transcribed word with its timing and owning sentence.
 * Callers must pass lists sorted ascending by [startMs] to [SentenceResolver].
 */
data class WordTiming(
    val word: String,
    val startMs: Long,
    val endMs: Long,
    val sentenceId: String,
)
