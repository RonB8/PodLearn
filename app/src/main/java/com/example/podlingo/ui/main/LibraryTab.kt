package com.example.podlingo.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.podlingo.ui.library.SavedEpisodesContent
import com.example.podlingo.ui.playlists.PlaylistsContent
import com.example.podlingo.ui.podcastlist.PodcastsContent
import com.example.podlingo.ui.strings.LocalAppStrings
import kotlinx.coroutines.launch

/** The Library tab: your downloaded episodes, your subscribed shows, and your playlists - switched by tapping the sub-tab strip or swiping between them - safe to swipe now that the outer Home/Search/Library pager no longer responds to horizontal drags. */
@Composable
fun LibraryTab(
    onOpenEpisode: (String) -> Unit,
    onOpenPodcast: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val strings = LocalAppStrings.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text(strings.podcastsTab) },
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text(strings.showsTab) },
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                text = { Text(strings.playlistsTab) },
            )
        }
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> SavedEpisodesContent(onOpenEpisode = onOpenEpisode)
                1 -> PodcastsContent(onOpenPodcast = onOpenPodcast)
                else -> PlaylistsContent(onOpenPlaylist = onOpenPlaylist)
            }
        }
    }
}
