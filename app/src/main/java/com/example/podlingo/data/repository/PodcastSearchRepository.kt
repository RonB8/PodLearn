package com.example.podlingo.data.repository

import com.example.podlingo.data.remote.podcastsearch.PodcastSearchApi
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchMapper
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchResult
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface PodcastSearchRepository {
    suspend fun search(query: String): Result<List<PodcastSearchResult>>
}

class PodcastSearchRepositoryImpl @Inject constructor(
    private val api: PodcastSearchApi,
) : PodcastSearchRepository {

    // Catches anything, not just IOException/HttpException - a malformed response (parsing) or
    // any other unexpected failure here should surface as a NetworkError state, not crash the app.
    override suspend fun search(query: String): Result<List<PodcastSearchResult>> = withContext(Dispatchers.IO) {
        try {
            Result.success(PodcastSearchMapper.map(api.search(term = query)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
