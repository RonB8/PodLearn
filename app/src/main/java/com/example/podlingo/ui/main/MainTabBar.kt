package com.example.podlingo.ui.main

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.podlingo.ui.strings.LocalAppStrings
import kotlinx.coroutines.launch

/** The Home/Search/Library switcher, docked below [com.example.podlingo.ui.player.MiniPlayerBar]. */
@Composable
fun MainTabBar(pagerState: PagerState, modifier: Modifier = Modifier) {
    val strings = LocalAppStrings.current
    val tabs = listOf(
        Triple(strings.tabHome, Icons.Filled.Home, 0),
        Triple(strings.tabSearch, Icons.Filled.Search, 1),
        Triple(strings.tabLibrary, Icons.Filled.LibraryMusic, 2),
    )
    val scope = rememberCoroutineScope()
    NavigationBar(modifier = modifier) {
        tabs.forEach { (label, icon, page) ->
            NavigationBarItem(
                selected = pagerState.currentPage == page,
                onClick = { scope.launch { pagerState.animateScrollToPage(page) } },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}
