@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.podlingo.ui.addpodcast.SearchContent
import com.example.podlingo.ui.history.HomeContent

@Composable
fun MainScreen(
    pagerState: PagerState,
    onOpenSettings: () -> Unit,
    onOpenEpisode: (String) -> Unit,
    onPodcastAdded: (String) -> Unit,
    onOpenPodcast: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tabTitle(pagerState.currentPage)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            when (page) {
                0 -> HomeContent(onOpenEpisode = onOpenEpisode)
                1 -> SearchContent(onAdded = onPodcastAdded)
                else -> LibraryTab(onOpenPodcast = onOpenPodcast, onOpenPlaylist = onOpenPlaylist)
            }
        }
    }
}

private fun tabTitle(page: Int): String = when (page) {
    0 -> "Home"
    1 -> "Search"
    else -> "Library"
}
