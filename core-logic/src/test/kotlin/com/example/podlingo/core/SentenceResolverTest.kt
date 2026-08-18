package com.example.podlingo.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SentenceResolverTest {

    private val reactionDelayMs = 700L

    // Sentence "s1": two words, no trailing silence before the next sentence starts.
    // Sentence "s2": starts immediately after s1 ends.
    // Then a 5s ad-break gap before sentence "s3".
    private val words = listOf(
        WordTiming("Hello", startMs = 80_000, endMs = 80_500, sentenceId = "s1"),
        WordTiming("there", startMs = 80_500, endMs = 83_000, sentenceId = "s1"),
        WordTiming("New", startMs = 83_000, endMs = 83_400, sentenceId = "s2"),
        WordTiming("sentence", startMs = 83_400, endMs = 84_500, sentenceId = "s2"),
        WordTiming("begins", startMs = 84_500, endMs = 85_500, sentenceId = "s2"),
        // ad break: silence from 85_500 to 90_500 (5s)
        WordTiming("After", startMs = 90_500, endMs = 91_000, sentenceId = "s3"),
        WordTiming("break", startMs = 91_000, endMs = 91_500, sentenceId = "s3"),
    )

    @Test
    fun `pause well inside a sentence resolves to that sentence`() {
        // Raw pause at 84_800ms, effective = 84_100ms, inside s2's "sentence" word.
        val result = SentenceResolver.resolve(pauseTimeMs = 84_800, reactionDelayMs, words)
        assertEquals(SentenceResolution.Resolved("s2"), result)
    }

    @Test
    fun `pause just after a new sentence began resolves to the previous sentence (1_23 vs 1_29 style)`() {
        // Raw pause at 83_300ms - only 0.3s into s2. After the 0.7s reaction-delay offset,
        // effective time (82_600ms) falls back inside s1's last word, so we correctly attribute
        // the confusion to s1, not the sentence that had barely started.
        val result = SentenceResolver.resolve(pauseTimeMs = 83_300, reactionDelayMs, words)
        assertEquals(SentenceResolution.Resolved("s1"), result)
    }

    @Test
    fun `pause well into a new sentence resolves to that new sentence`() {
        // Raw pause at 89_000ms (6s after s2 started) - effective time (88_300ms) is still well
        // past s2's last word, so plenty of s2 had already played; it should not fall back to s1.
        val result = SentenceResolver.resolve(pauseTimeMs = 89_000, reactionDelayMs, words)
        assertEquals(SentenceResolution.Resolved("s2"), result)
    }

    @Test
    fun `pause in a silence gap beyond the threshold returns no relevant sentence`() {
        // Raw pause at 89_500ms, effective = 88_800ms - 3_300ms after s2 ended (85_500),
        // still within the default 4500ms max gap, so it should resolve to s2 (not silence yet).
        // Push further into the gap to actually exceed the threshold:
        val result = SentenceResolver.resolve(pauseTimeMs = 90_800, reactionDelayMs, words)
        // effective = 90_100ms, 4_600ms after s2 ended (85_500) -> exceeds default 4500ms max gap.
        assertEquals(SentenceResolution.NoRelevantSentence, result)
    }

    @Test
    fun `pause in a silence gap within the threshold still resolves to the preceding sentence`() {
        // Raw pause at 89_900ms, effective = 89_200ms - 3_700ms after s2 ended: within max gap.
        val result = SentenceResolver.resolve(pauseTimeMs = 89_900, reactionDelayMs, words)
        assertEquals(SentenceResolution.Resolved("s2"), result)
    }

    @Test
    fun `gap exactly at the threshold still resolves (not strictly greater)`() {
        val customMaxGap = 4000L
        // s2's last word ends at 85_500. Effective time exactly 4000ms after that -> boundary.
        val effectiveTimeMs = 89_500L
        val result = SentenceResolver.resolve(
            pauseTimeMs = effectiveTimeMs + reactionDelayMs,
            reactionDelayMs = reactionDelayMs,
            words = words,
            maxGapMs = customMaxGap,
        )
        assertEquals(SentenceResolution.Resolved("s2"), result)
    }

    @Test
    fun `effective time before the first word resolves to it when close enough`() {
        val result = SentenceResolver.resolve(pauseTimeMs = 80_200, reactionDelayMs = 700, words = words)
        // effective = 79_500ms, 500ms before the first word (80_000) - within default max gap.
        assertEquals(SentenceResolution.Resolved("s1"), result)
    }

    @Test
    fun `effective time far before the first word returns no relevant sentence`() {
        val result = SentenceResolver.resolve(pauseTimeMs = 2_000, reactionDelayMs = 700, words = words)
        assertEquals(SentenceResolution.NoRelevantSentence, result)
    }

    @Test
    fun `pause after the last word within the gap resolves to the last sentence`() {
        val result = SentenceResolver.resolve(pauseTimeMs = 92_000, reactionDelayMs = 700, words = words)
        assertEquals(SentenceResolution.Resolved("s3"), result)
    }

    @Test
    fun `empty word list returns no relevant sentence`() {
        val result = SentenceResolver.resolve(pauseTimeMs = 10_000, reactionDelayMs = 700, words = emptyList())
        assertEquals(SentenceResolution.NoRelevantSentence, result)
    }

    @Test
    fun `single word timeline resolves correctly`() {
        val single = listOf(WordTiming("Hi", startMs = 1000, endMs = 1200, sentenceId = "only"))
        val result = SentenceResolver.resolve(pauseTimeMs = 1900, reactionDelayMs = 700, words = single)
        assertEquals(SentenceResolution.Resolved("only"), result)
    }
}
