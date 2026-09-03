@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.episodelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.TranscriptStatus
import com.example.podlingo.ui.playlists.AddToPlaylistDialog
import com.example.podlingo.ui.strings.AppStrings
import com.example.podlingo.ui.strings.LocalAppStrings
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EpisodeListScreen(
    onOpenEpisode: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: EpisodeListViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    var addToPlaylistEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDownloadedOnly by rememberSaveable { mutableStateOf(false) }

    addToPlaylistEpisodeId?.let { episodeId ->
        AddToPlaylistDialog(episodeId = episodeId, onDismiss = { addToPlaylistEpisodeId = null })
    }

    val displayedEpisodes = if (showDownloadedOnly) episodes.filter { it.localFilePath != null } else episodes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.episodesTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            EpisodeFilterRow(
                showDownloadedOnly = showDownloadedOnly,
                onSelected = { showDownloadedOnly = it },
                strings = strings,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (displayedEpisodes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.noEpisodesFound)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayedEpisodes, key = { it.id }) { episode ->
                            ListItem(
                                headlineContent = { Text(episode.title) },
                                supportingContent = { EpisodeSupportingText(episode, strings) },
                                trailingContent = {
                                    IconButton(onClick = { addToPlaylistEpisodeId = episode.id }) {
                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = strings.addToPlaylist)
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
    }
}

@Composable
private fun EpisodeFilterRow(
    showDownloadedOnly: Boolean,
    onSelected: (Boolean) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
    val options = listOf(false to strings.episodeFilterAll, true to strings.episodeFilterDownloaded)
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = showDownloadedOnly == value,
                onClick = { onSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun EpisodeSupportingText(episode: EpisodeEntity, strings: AppStrings) {
    Column {
        val metaLine = listOfNotNull(
            episode.pubDateEpochMs?.let { formatPubDate(it) },
            episode.durationSec?.let { formatDuration(it) },
        ).joinToString(" • ")
        if (metaLine.isNotEmpty()) {
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(transcriptStatusLabel(episode.transcriptStatus, strings))
    }
}

private val pubDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

private fun formatPubDate(epochMs: Long): String = pubDateFormat.format(epochMs)

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs) else "%d:%02d".format(minutes, secs)
}

private fun transcriptStatusLabel(status: TranscriptStatus, strings: AppStrings): String = when (status) {
    TranscriptStatus.NONE -> strings.transcriptNotYetProcessed
    TranscriptStatus.DOWNLOADING -> strings.transcriptDownloading
    TranscriptStatus.TRANSCRIBING -> strings.transcriptTranscribing
    TranscriptStatus.PROCESSING -> strings.transcriptProcessing
    TranscriptStatus.READY -> strings.ready
    TranscriptStatus.FAILED -> strings.transcriptFailedTapToRetry
}
