package com.example.podlingo.data.repository

import com.example.podlingo.data.remote.podcastsearch.PodcastSearchApi
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchMapper
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchResult
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

interface PodcastSearchRepository {
    suspend fun search(query: String): Result<List<PodcastSearchResult>>
}

class PodcastSearchRepositoryImpl @Inject constructor(
    private val api: PodcastSearchApi,
) : PodcastSearchRepository {

    override suspend fun search(query: String): Result<List<PodcastSearchResult>> = withContext(Dispatchers.IO) {
        try {
            Result.success(PodcastSearchMapper.map(api.search(term = query)))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }
    }
}
