package com.example.podlingo.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.podlingo.ui.playlists.PlaylistsContent
import com.example.podlingo.ui.podcastlist.PodcastsContent

/**
 * The Library tab: your subscribed podcasts and your playlists, switched with a click-based
 * sub-tab strip (not swipe) so it doesn't fight the outer Home/Search/Library pager's gesture.
 */
@Composable
fun LibraryTab(
    onOpenPodcast: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Podcasts") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Playlists") })
        }
        when (subTab) {
            0 -> PodcastsContent(onOpenPodcast = onOpenPodcast)
            else -> PlaylistsContent(onOpenPlaylist = onOpenPlaylist)
        }
    }
}
