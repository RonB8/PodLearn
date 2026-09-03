@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.history

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.data.local.dao.RecentlyPlayedItem
import com.example.podlingo.data.local.dao.RecentlyPlayedPodcast
import com.example.podlingo.ui.common.ArtworkThumbnail
import com.example.podlingo.ui.playlists.AddToPlaylistDialog
import com.example.podlingo.ui.strings.AppStrings
import com.example.podlingo.ui.strings.LocalAppStrings
import com.example.podlingo.ui.vocabulary.VocabQuizDialog
import java.util.concurrent.TimeUnit

/** The Home tab: your episode history and, in a second sub-tab, the podcasts behind it. Pull down to refresh every subscribed podcast's feed for new episodes. */
@Composable
fun HomeContent(
    onOpenEpisode: (String) -> Unit,
    onOpenPodcast: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val recentlyPlayedPodcasts by viewModel.recentlyPlayedPodcasts.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val quiz by viewModel.quiz.collectAsStateWithLifecycle()
    var addToPlaylistEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }
    var removingEpisodeId by rememberSaveable { mutableStateOf<String?>(null) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.noUnknownWordsEvent.collect {
            Toast.makeText(context, strings.noUnknownWordsToQuizMessage, Toast.LENGTH_SHORT).show()
        }
    }

    addToPlaylistEpisodeId?.let { episodeId ->
        AddToPlaylistDialog(episodeId = episodeId, onDismiss = { addToPlaylistEpisodeId = null })
    }
    removingEpisodeId?.let { episodeId ->
        AlertDialog(
            onDismissRequest = { removingEpisodeId = null },
            title = { Text(strings.removeFromHistoryConfirmTitle) },
            text = { Text(strings.removeFromHistoryConfirmText) },
            confirmButton = {
                TextButton(onClick = { viewModel.removeFromHistory(episodeId); removingEpisodeId = null }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { removingEpisodeId = null }) { Text(strings.cancel) }
            },
        )
    }
    quiz?.let { quizState ->
        VocabQuizDialog(
            quiz = quizState,
            onAnswerSelected = viewModel::onQuizAnswerSelected,
            onNext = viewModel::onQuizNext,
            onDismiss = viewModel::onQuizDismissed,
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(strings.historyTab) },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(strings.podcastsTab) },
            )
        }
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = viewModel::refreshAll,
            modifier = Modifier.fillMaxSize(),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> EpisodeHistoryList(
                        episodes = recentlyPlayed,
                        strings = strings,
                        onOpenEpisode = onOpenEpisode,
                        onAddToPlaylist = { addToPlaylistEpisodeId = it },
                        onQuiz = viewModel::startQuiz,
                        onRemove = { removingEpisodeId = it },
                    )
                    else -> PodcastHistoryList(podcasts = recentlyPlayedPodcasts, strings = strings, onOpenPodcast = onOpenPodcast)
                }
            }
        }
    }
}

@Composable
private fun EpisodeHistoryList(
    episodes: List<RecentlyPlayedItem>,
    strings: AppStrings,
    onOpenEpisode: (String) -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onQuiz: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    if (episodes.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = strings.noEpisodesPlayedYet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(episodes, key = { it.id }) { item ->
                var showMenu by remember { mutableStateOf(false) }
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
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = strings.moreOptionsContentDescription)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(strings.addToPlaylist) },
                                    onClick = { showMenu = false; onAddToPlaylist(item.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.quizMenuItem) },
                                    onClick = { showMenu = false; onQuiz(item.id) },
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.delete) },
                                    onClick = { showMenu = false; onRemove(item.id) },
                                )
                            }
                        }
                    },
                    modifier = Modifier.clickable { onOpenEpisode(item.id) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun PodcastHistoryList(
    podcasts: List<RecentlyPlayedPodcast>,
    strings: AppStrings,
    onOpenPodcast: (String) -> Unit,
) {
    if (podcasts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = strings.noPodcastsPlayedYet,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(podcasts, key = { it.id }) { podcast ->
                ListItem(
                    leadingContent = { ArtworkThumbnail(artworkUrl = podcast.artworkUrl) },
                    headlineContent = { Text(podcast.title) },
                    supportingContent = {
                        Text(
                            text = relativeTime(podcast.lastPlayedEpochMs, strings),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier.clickable { onOpenPodcast(podcast.id) },
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
