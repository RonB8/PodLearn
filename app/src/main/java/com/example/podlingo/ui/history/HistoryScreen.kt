@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.util.concurrent.TimeUnit

@Composable
fun HistoryScreen(
    onOpenEpisode: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (recentlyPlayed.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = "No episodes played yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(recentlyPlayed, key = { it.id }) { item ->
                    ListItem(
                        leadingContent = { ArtworkThumbnail(artworkUrl = item.artworkUrl) },
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.podcastTitle) },
                        trailingContent = {
                            Text(
                                text = relativeTime(item.lastPlayedEpochMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        modifier = Modifier.clickable { onOpenEpisode(item.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun relativeTime(epochMs: Long): String {
    val elapsedMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMs)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsedMs)
    val days = TimeUnit.MILLISECONDS.toDays(elapsedMs)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
