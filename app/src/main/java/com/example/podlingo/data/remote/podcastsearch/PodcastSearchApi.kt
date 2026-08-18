package com.example.podlingo.data.remote.podcastsearch

import retrofit2.http.GET
import retrofit2.http.Query

interface PodcastSearchApi {

    /** Calls iTunes's unauthenticated `GET /search` podcast search endpoint. */
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("entity") entity: String = "podcast",
        @Query("limit") limit: Int = 25,
    ): ITunesSearchResponse
}
