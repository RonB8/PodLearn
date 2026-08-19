package com.example.podlingo.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.podlingo.ui.addpodcast.AddPodcastScreen
import com.example.podlingo.ui.episodelist.EpisodeListScreen
import com.example.podlingo.ui.history.HistoryScreen
import com.example.podlingo.ui.player.MiniPlayerBar
import com.example.podlingo.ui.player.NowPlayingViewModel
import com.example.podlingo.ui.player.PlayerScreen
import com.example.podlingo.ui.playlists.PlaylistDetailScreen
import com.example.podlingo.ui.playlists.PlaylistsScreen
import com.example.podlingo.ui.podcastlist.PodcastListScreen
import com.example.podlingo.ui.settings.SettingsScreen

/** Argument-free top-level screens worth restoring to on a cold start (see [AppNavigationViewModel]). */
private val RESTORABLE_ROUTES = setOf(Routes.PODCASTS, Routes.HISTORY, Routes.SETTINGS, Routes.PLAYLISTS)

@Composable
fun PodLingoNavHost(navController: NavHostController = rememberNavController()) {
    val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
    val nowPlaying by nowPlayingViewModel.nowPlaying.collectAsStateWithLifecycle()
    val appNavigationViewModel: AppNavigationViewModel = hiltViewModel()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(Unit) {
        val restoreRoute = appNavigationViewModel.lastRoute
        if (restoreRoute != null && restoreRoute != Routes.PODCASTS && restoreRoute in RESTORABLE_ROUTES) {
            navController.navigate(restoreRoute)
        }
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute in RESTORABLE_ROUTES) {
            appNavigationViewModel.rememberRoute(currentRoute)
        }
    }

    Scaffold(
        // Each inner screen's own Scaffold already handles the top status-bar inset, and
        // MiniPlayerBar handles the bottom nav-bar inset itself (navigationBarsPadding) - without
        // this, Scaffold's default systemBars inset reserves that bottom space *again* on top of
        // the bar's own (now taller) measured height, leaving a blank gap above the bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (nowPlaying != null && currentRoute != Routes.PLAYER) {
                MiniPlayerBar(
                    nowPlaying = nowPlaying!!,
                    onClick = {
                        navController.navigate(Routes.player(nowPlaying!!.episodeId)) { launchSingleTop = true }
                    },
                    onTogglePlayPause = nowPlayingViewModel::togglePlayPause,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PODCASTS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.PODCASTS) {
                PodcastListScreen(
                    onAddPodcast = { navController.navigate(Routes.ADD_PODCAST) { launchSingleTop = true } },
                    onOpenPodcast = { podcastId ->
                        navController.navigate(Routes.episodes(podcastId)) { launchSingleTop = true }
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) { launchSingleTop = true } },
                    onOpenPlaylists = { navController.navigate(Routes.PLAYLISTS) { launchSingleTop = true } },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onOpenEpisode = { episodeId ->
                        navController.navigate(Routes.player(episodeId)) { launchSingleTop = true }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PLAYLISTS) {
                PlaylistsScreen(
                    onOpenPlaylist = { playlistId ->
                        navController.navigate(Routes.playlistDetail(playlistId)) { launchSingleTop = true }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.PLAYLIST_DETAIL,
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
            ) {
                PlaylistDetailScreen(
                    onOpenEpisode = { episodeId ->
                        navController.navigate(Routes.player(episodeId)) { launchSingleTop = true }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ADD_PODCAST) {
                AddPodcastScreen(
                    onAdded = { podcastId ->
                        navController.navigate(Routes.episodes(podcastId)) {
                            popUpTo(Routes.PODCASTS)
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.EPISODES,
                arguments = listOf(navArgument("podcastId") { type = NavType.StringType }),
            ) {
                EpisodeListScreen(
                    onOpenEpisode = { episodeId ->
                        navController.navigate(Routes.player(episodeId)) { launchSingleTop = true }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.PLAYER,
                arguments = listOf(navArgument("episodeId") { type = NavType.StringType }),
            ) {
                PlayerScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToEpisode = { episodeId ->
                        navController.navigate(Routes.player(episodeId)) {
                            popUpTo(Routes.PLAYER) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
