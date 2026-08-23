package com.example.podlingo.core

import org.junit.Assert.assertEquals
import org.junit.Test

class WordDifficultyRankerTest {

    private val ranks = mapOf("the" to 0, "cat" to 0, "sat" to 1, "obfuscate" to 4, "welcome" to 3)
    private fun rankOf(word: String): Int = ranks[word.lowercase()] ?: 5
    private fun isKnownWord(word: String): Boolean = ranks.containsKey(word.lowercase())

    private fun orderHardestFirst(words: List<WordTiming>) =
        WordDifficultyRanker.orderHardestFirst(words, ::rankOf, ::isKnownWord)

    @Test
    fun `orders words hardest first, ties keep original order`() {
        val words = listOf(
            WordTiming("The", 0, 100, "s1"),
            WordTiming("cat", 100, 200, "s1"),
            WordTiming("sat", 200, 300, "s1"),
            WordTiming("obfuscate", 300, 400, "s1"),
            WordTiming("mysteriously", 400, 500, "s1"), // not ranked -> hardest (rank 5)
        )

        val ordered = orderHardestFirst(words)

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

        val ordered = orderHardestFirst(words)

        assertEquals(listOf("word"), ordered.map { it.word })
    }

    @Test
    fun `drops capitalized unrecognized words as names, wherever they fall in the sentence`() {
        // "Ram" and "Israeli" are capitalized and unranked (rank 5) -> dropped as names. "Samaria"
        // is too, even though it's the *first* word here - transcripts are chunked into segments
        // at pause points, not true sentence boundaries, so a name can end up "sentence-initial"
        // purely by where the transcription happened to split; position alone can't be trusted.
        val words = listOf(
            WordTiming("Samaria", 0, 100, "s1"),
            WordTiming("or", 100, 200, "s1"),
            WordTiming("Ram", 200, 300, "s1"),
            WordTiming("in", 300, 400, "s1"),
            WordTiming("Israeli", 400, 500, "s1"),
        )

        val ordered = orderHardestFirst(words)

        assertEquals(listOf("or", "in"), ordered.map { it.word })
    }

    @Test
    fun `keeps a capitalized word eligible when it's actually in the vocabulary`() {
        // "Welcome" is capitalized only because it opens the sentence - it's a real, ranked
        // dictionary word, so ordinary capitalization there doesn't mean it's a name.
        val words = listOf(
            WordTiming("Welcome", 0, 100, "s1"),
            WordTiming("home", 100, 200, "s1"), // unranked -> hardest (rank 5)
        )

        val ordered = orderHardestFirst(words)

        assertEquals(listOf("home", "Welcome"), ordered.map { it.word })
    }
}
