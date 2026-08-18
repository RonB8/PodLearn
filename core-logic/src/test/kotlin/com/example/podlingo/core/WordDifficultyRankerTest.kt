package com.example.podlingo.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WordDifficultyRankerTest {

    private val ranks = mapOf("the" to 0, "cat" to 0, "sat" to 1, "obfuscate" to 4)
    private fun rankOf(word: String): Int = ranks[word.lowercase()] ?: 5

    @Test
    fun `orders words hardest first, ties keep original order`() {
        val words = listOf(
            WordTiming("The", 0, 100, "s1"),
            WordTiming("cat", 100, 200, "s1"),
            WordTiming("sat", 200, 300, "s1"),
            WordTiming("obfuscate", 300, 400, "s1"),
            WordTiming("mysteriously", 400, 500, "s1"), // not ranked -> hardest (rank 5)
        )

        val ordered = WordDifficultyRanker.orderHardestFirst(words) { rankOf(it) }

        assertEquals(
            listOf("mysteriously", "obfuscate", "sat", "The", "cat"),
            ordered.map { it.word },
        )
    }

    @Test
    fun `drops punctuation-only tokens`() {
        val words = listOf(
            WordTiming("--", 0, 50, "s1"),
            WordTiming("word", 50, 150, "s1"),
        )

        val ordered = WordDifficultyRanker.orderHardestFirst(words) { 0 }

        assertEquals(listOf("word"), ordered.map { it.word })
    }
}
