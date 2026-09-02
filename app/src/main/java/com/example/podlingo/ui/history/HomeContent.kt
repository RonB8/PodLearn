package com.example.podlingo.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.ui.common.ArtworkThumbnail
import com.example.podlingo.ui.playlists.AddToPlaylistDialog
import com.example.podlingo.ui.strings.AppStrings
import com.example.podlingo.ui.strings.LocalAppStrings
import java.util.concurrent.TimeUnit

/** The Home tab: "continue listening" - your most recently played episodes. */
@Composable
fun HomeContent(
    onOpenEpisode: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    var addToPlaylistEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }

    addToPlaylistEpisodeId?.let { episodeId ->
        AddToPlaylistDialog(episodeId = episodeId, onDismiss = { addToPlaylistEpisodeId = null })
    }

    if (recentlyPlayed.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = strings.noEpisodesPlayedYet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(recentlyPlayed, key = { it.id }) { item ->
                ListItem(
                    leadingContent = { ArtworkThumbnail(artworkUrl = item.artworkUrl) },
                    headlineContent = { Text(item.title) },
                    supportingContent = {
                        Column {
                            Text(item.podcastTitle)
                            Text(
                                text = relativeTime(item.lastPlayedEpochMs, strings),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { addToPlaylistEpisodeId = item.id }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = strings.addToPlaylist)
                        }
                    },
                    modifier = Modifier.clickable { onOpenEpisode(item.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

private fun relativeTime(epochMs: Long, strings: AppStrings): String {
    val elapsedMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMs)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMs)
    return when {
        minutes < 1 -> strings.justNow
        minutes < 60 -> strings.minutesAgo(minutes)
        hours < 24 -> strings.hoursAgo(hours)
        days < 7 -> strings.daysAgo(days)
        else -> strings.weeksAgo(days / 7)
    }
}
