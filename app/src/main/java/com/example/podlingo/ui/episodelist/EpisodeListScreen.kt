@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.episodelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.data.local.entity.TranscriptStatus
import com.example.podlingo.ui.playlists.AddToPlaylistDialog

@Composable
fun EpisodeListScreen(
    onOpenEpisode: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: EpisodeListViewModel = hiltViewModel(),
) {
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    var addToPlaylistEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }

    addToPlaylistEpisodeId?.let { episodeId ->
        AddToPlaylistDialog(episodeId = episodeId, onDismiss = { addToPlaylistEpisodeId = null })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Episodes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (episodes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No episodes found in this feed.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(episodes, key = { it.id }) { episode ->
                    ListItem(
                        headlineContent = { Text(episode.title) },
                        supportingContent = { Text(transcriptStatusLabel(episode.transcriptStatus)) },
                        trailingContent = {
                            IconButton(onClick = { addToPlaylistEpisodeId = episode.id }) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
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

private fun transcriptStatusLabel(status: TranscriptStatus): String = when (status) {
    TranscriptStatus.NONE -> "Not yet processed"
    TranscriptStatus.DOWNLOADING -> "Downloading..."
    TranscriptStatus.TRANSCRIBING -> "Transcribing..."
    TranscriptStatus.PROCESSING -> "Processing..."
    TranscriptStatus.READY -> "Ready"
    TranscriptStatus.FAILED -> "Failed - tap to retry"
}
