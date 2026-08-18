package com.example.podlingo.data.repository

import com.example.podlingo.data.remote.podcastsearch.ITunesPodcastDto
import com.example.podlingo.data.remote.podcastsearch.ITunesSearchResponse
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchApi
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastSearchRepositoryImplTest {

    @Test
    fun `successful api call maps to a result list`() = runTest {
        val api = FakePodcastSearchApi {
            ITunesSearchResponse(
                results = listOf(ITunesPodcastDto(collectionName = "Show", feedUrl = "https://example.com/feed.xml")),
            )
        }
        val repository = PodcastSearchRepositoryImpl(api)

        val result = repository.search("query")

        assertTrue(result.isSuccess)
        assertEquals(listOf("Show"), result.getOrThrow().map { it.title })
    }

    @Test
    fun `network failure surfaces as a Result failure instead of throwing`() = runTest {
        val api = FakePodcastSearchApi { throw IOException("offline") }
        val repository = PodcastSearchRepositoryImpl(api)

        val result = repository.search("query")

        assertTrue(result.isFailure)
        assertEquals("offline", result.exceptionOrNull()?.message)
    }

    private class FakePodcastSearchApi(private val response: () -> ITunesSearchResponse) : PodcastSearchApi {
        override suspend fun search(term: String, media: String, entity: String, limit: Int): ITunesSearchResponse =
            response()
    }
}
