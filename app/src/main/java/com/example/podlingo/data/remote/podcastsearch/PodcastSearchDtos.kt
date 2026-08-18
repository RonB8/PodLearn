package com.example.podlingo.data.remote.podcastsearch

import kotlinx.serialization.Serializable

@Serializable
data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesPodcastDto> = emptyList(),
)

@Serializable
data class ITunesPodcastDto(
    val collectionName: String? = null,
    val artistName: String? = null,
    val feedUrl: String? = null,
    val artworkUrl600: String? = null,
    val primaryGenreName: String? = null,
    val trackCount: Int? = null,
)
