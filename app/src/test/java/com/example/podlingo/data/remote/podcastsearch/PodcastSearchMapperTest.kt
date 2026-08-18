package com.example.podlingo.data.remote.podcastsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastSearchMapperTest {

    @Test
    fun `maps a full result`() {
        val response = ITunesSearchResponse(
            resultCount = 1,
            results = listOf(
                ITunesPodcastDto(
                    collectionName = "Test Show",
                    artistName = "Test Author",
                    feedUrl = "https://example.com/feed.xml",
                    artworkUrl600 = "https://example.com/art.jpg",
                    primaryGenreName = "Technology",
                    trackCount = 42,
                ),
            ),
        )

        val results = PodcastSearchMapper.map(response)

        assertEquals(1, results.size)
        val result = results.single()
        assertEquals("https://example.com/feed.xml", result.feedUrl)
        assertEquals("Test Show", result.title)
        assertEquals("Test Author", result.author)
        assertEquals("https://example.com/art.jpg", result.artworkUrl)
        assertEquals("Technology", result.genre)
        assertEquals(42, result.episodeCount)
    }

    @Test
    fun `drops results missing a feed url`() {
        val response = ITunesSearchResponse(
            results = listOf(
                ITunesPodcastDto(collectionName = "No Feed", feedUrl = null),
                ITunesPodcastDto(collectionName = "Blank Feed", feedUrl = "   "),
                ITunesPodcastDto(collectionName = "Has Feed", feedUrl = "https://example.com/feed.xml"),
            ),
        )

        val results = PodcastSearchMapper.map(response)

        assertEquals(listOf("Has Feed"), results.map { it.title })
    }

    @Test
    fun `falls back to feed url for a blank title and empty string for a missing author`() {
        val response = ITunesSearchResponse(
            results = listOf(
                ITunesPodcastDto(collectionName = "  ", artistName = null, feedUrl = "https://example.com/feed.xml"),
            ),
        )

        val result = PodcastSearchMapper.map(response).single()

        assertEquals("https://example.com/feed.xml", result.title)
        assertEquals("", result.author)
    }

    @Test
    fun `empty response maps to an empty list`() {
        assertTrue(PodcastSearchMapper.map(ITunesSearchResponse()).isEmpty())
    }
}
