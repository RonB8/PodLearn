package com.example.podlingo.data.remote.podcastsearch

data class PodcastSearchResult(
    val feedUrl: String,
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val genre: String?,
    val episodeCount: Int?,
)

/**
 * Maps iTunes's search response to [PodcastSearchResult]s. iTunes mixes in non-podcast or
 * malformed entries for some queries, so any result missing a feed URL (the one field the rest
 * of the app can't do without) is dropped rather than surfaced as a broken result.
 */
object PodcastSearchMapper {

    fun map(response: ITunesSearchResponse): List<PodcastSearchResult> =
        response.results.mapNotNull { dto ->
            val feedUrl = dto.feedUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            PodcastSearchResult(
                feedUrl = feedUrl,
                title = dto.collectionName?.trim()?.takeIf { it.isNotEmpty() } ?: feedUrl,
                author = dto.artistName?.trim().orEmpty(),
                artworkUrl = dto.artworkUrl600,
                genre = dto.primaryGenreName,
                episodeCount = dto.trackCount,
            )
        }
}
