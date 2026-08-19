package com.example.podlingo.ui.podcastlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.ui.common.ArtworkThumbnail

/** The "Podcasts" section within the Library tab. Adding a podcast now happens via Search. */
@Composable
fun PodcastsContent(
    onOpenPodcast: (String) -> Unit,
    viewModel: PodcastListViewModel = hiltViewModel(),
) {
    val podcasts by viewModel.podcasts.collectAsStateWithLifecycle()

    if (podcasts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No podcasts yet. Find one in Search.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(podcasts, key = { it.id }) { podcast ->
                ListItem(
                    leadingContent = { ArtworkThumbnail(artworkUrl = podcast.imageUrl) },
                    headlineContent = { Text(podcast.title) },
                    modifier = Modifier.clickable { onOpenPodcast(podcast.id) },
                )
                HorizontalDivider()
            }
        }
    }
}
