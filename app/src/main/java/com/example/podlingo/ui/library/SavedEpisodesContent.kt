package com.example.podlingo.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import com.example.podlingo.data.local.dao.SavedEpisodeItem
import com.example.podlingo.ui.common.ArtworkThumbnail
import com.example.podlingo.ui.common.ConfirmDialog
import com.example.podlingo.ui.common.EpisodeRowMenu
import com.example.podlingo.ui.common.formatDuration
import com.example.podlingo.ui.common.formatPubDate
import com.example.podlingo.ui.playlists.AddToPlaylistDialog
import com.example.podlingo.ui.strings.LocalAppStrings
import com.example.podlingo.ui.vocabulary.EpisodeQuizHost

/** The "Saved" section within the Library tab - every downloaded episode across all podcasts, newest published first. */
@Composable
fun SavedEpisodesContent(
    onOpenEpisode: (String) -> Unit,
    viewModel: SavedEpisodesViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val quiz by viewModel.quiz.collectAsStateWithLifecycle()
    var addToPlaylistEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var removingDownloadEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }

    EpisodeQuizHost(
        quiz = quiz,
        noUnknownWordsEvent = viewModel.noUnknownWordsEvent,
        onAnswerSelected = viewModel::onQuizAnswerSelected,
        onNext = viewModel::onQuizNext,
        onDismiss = viewModel::onQuizDismissed,
    )

    addToPlaylistEpisodeId?.let { episodeId ->
        AddToPlaylistDialog(episodeId = episodeId, onDismiss = { addToPlaylistEpisodeId = null })
    }
    removingDownloadEpisodeId?.let { episodeId ->
        ConfirmDialog(
            title = strings.removeDownloadConfirmTitle,
            text = strings.removeDownloadConfirmText,
            confirmLabel = strings.delete,
            onConfirm = { viewModel.removeDownload(episodeId); removingDownloadEpisodeId = null },
            onDismiss = { removingDownloadEpisodeId = null },
        )
    }

    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = strings.noSavedEpisodesYet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(episodes, key = { it.id }) { episode ->
                ListItem(
                    leadingContent = { ArtworkThumbnail(artworkUrl = episode.artworkUrl) },
                    headlineContent = { Text(episode.title) },
                    supportingContent = { SavedEpisodeSupportingText(episode) },
                    trailingContent = {
                        EpisodeRowMenu(
                            onAddToPlaylist = { addToPlaylistEpisodeId = episode.id },
                            onQuiz = { viewModel.startQuiz(episode.id) },
                            onDelete = { removingDownloadEpisodeId = episode.id },
                        )
                    },
                    modifier = Modifier.clickable { onOpenEpisode(episode.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SavedEpisodeSupportingText(episode: SavedEpisodeItem) {
    Column {
        Text(episode.podcastTitle)
        val metaLine = listOfNotNull(
            episode.pubDateEpochMs?.let { formatPubDate(it) },
            episode.durationSec?.let { formatDuration(it) },
        ).joinToString(" • ")
        if (metaLine.isNotEmpty()) {
            Text(
                text = metaLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
