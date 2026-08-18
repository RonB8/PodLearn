package com.example.podlingo.testutil

import com.example.podlingo.data.remote.podcastsearch.PodcastSearchResult
import com.example.podlingo.data.repository.PodcastSearchRepository

class FakePodcastSearchRepository : PodcastSearchRepository {

    var result: Result<List<PodcastSearchResult>> = Result.success(emptyList())

    var callCount = 0
        private set
    var lastQuery: String? = null
        private set

    override suspend fun search(query: String): Result<List<PodcastSearchResult>> {
        callCount++
        lastQuery = query
        return result
    }
}
