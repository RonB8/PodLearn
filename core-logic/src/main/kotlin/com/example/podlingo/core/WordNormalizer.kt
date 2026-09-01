package com.example.podlingo.core

/**
 * Canonical lookup key for a word, shared by every feature that needs to key on "the same word"
 * regardless of surface casing/punctuation - CEFR difficulty lookup, and per-user vocabulary
 * knowledge tracking. Keeping this in one place means those features can never key differently
 * for the same word.
 */
object WordNormalizer {
    fun normalize(word: String): String =
        word.lowercase().filter { it.isLetter() || it == '\'' || it == '-' }
}
