package com.example.podlearn.data.remote.rss

data class RssFeed(
    val title: String,
    val imageUrl: String?,
    val description: String?,
    val items: List<RssItem>,
)

data class RssItem(
    val guid: String?,
    val title: String,
    val pubDateEpochMs: Long?,
    val audioUrl: String?,
    val durationSec: Long?,
)
