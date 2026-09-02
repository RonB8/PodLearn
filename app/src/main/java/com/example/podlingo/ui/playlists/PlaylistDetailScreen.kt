@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.ui.common.ArtworkThumbnail
import com.example.podlingo.ui.strings.LocalAppStrings

@Composable
fun PlaylistDetailScreen(
    onOpenEpisode: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val playlistName by viewModel.playlistName.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName.ifBlank { strings.playlistFallbackTitle }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = strings.noEpisodesInPlaylist,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(episodes, key = { it.id }) { episode ->
                    ListItem(
                        leadingContent = { ArtworkThumbnail(artworkUrl = episode.artworkUrl) },
                        headlineContent = { Text(episode.title) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.removeEpisode(episode.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = strings.removeFromPlaylistContentDescription)
                            }
                        },
                        modifier = Modifier.clickable { onOpenEpisode(episode.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
