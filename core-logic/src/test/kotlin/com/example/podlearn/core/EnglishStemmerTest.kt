package com.example.podlearn.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishStemmerTest {

    private fun stems(word: String) = EnglishStemmer.candidateBaseForms(word)

    @Test
    fun `plain plural strips trailing s`() {
        assertTrue("member" in stems("members"))
        assertTrue("cat" in stems("cats"))
    }

    @Test
    fun `e-plus-s plural strips only the s`() {
        assertTrue("vote" in stems("votes"))
        assertTrue("close" in stems("closes"))
    }

    @Test
    fun `es plural strips es`() {
        assertTrue("box" in stems("boxes"))
    }

    @Test
    fun `ies plural becomes y`() {
        assertTrue("category" in stems("categories"))
    }

    @Test
    fun `past tense with silent e restores it`() {
        assertTrue("vote" in stems("voted"))
    }

    @Test
    fun `past tense without silent e needs no restoration`() {
        assertTrue("want" in stems("wanted"))
    }

    @Test
    fun `past tense with doubled consonant is undoubled`() {
        assertTrue("stop" in stems("stopped"))
    }

    @Test
    fun `gerund with silent e restores it`() {
        assertTrue("vote" in stems("voting"))
        assertTrue("close" in stems("closing"))
    }

    @Test
    fun `gerund with doubled consonant is undoubled`() {
        assertTrue("run" in stems("running"))
    }

    @Test
    fun `comparative and superlative restore silent e`() {
        assertTrue("nice" in stems("nicer"))
        assertTrue("nice" in stems("nicest"))
        assertTrue("old" in stems("older"))
    }

    @Test
    fun `possessive strips the trailing 's`() {
        assertTrue("today" in stems("today's"))
    }

    @Test
    fun `short words and plain base forms yield no misleading noise`() {
        assertTrue(stems("it").isEmpty())
        assertFalse("clos" in stems("close")) // "close" itself isn't stemmed further
    }
}
