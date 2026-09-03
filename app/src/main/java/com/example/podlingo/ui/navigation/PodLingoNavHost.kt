package com.example.podlingo.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.podlingo.ui.episodelist.EpisodeListScreen
import com.example.podlingo.ui.main.MainScreen
import com.example.podlingo.ui.main.MainTabBar
import com.example.podlingo.ui.player.DownloadingBanner
import com.example.podlingo.ui.player.MiniPlayerBar
import com.example.podlingo.ui.player.NowPlayingViewModel
import com.example.podlingo.ui.player.PlayerScreen
import com.example.podlingo.ui.playlists.PlaylistDetailScreen
import com.example.podlingo.ui.settings.SettingsScreen
import com.example.podlingo.ui.vocabulary.UnknownWordsScreen

/** Argument-free top-level screens worth restoring to on a cold start (see [AppNavigationViewModel]). */
private val RESTORABLE_ROUTES = setOf(Routes.MAIN, Routes.SETTINGS)

@Composable
fun PodLingoNavHost(navController: NavHostController = rememberNavController()) {
    val nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
    val nowPlaying by nowPlayingViewModel.nowPlaying.collectAsStateWithLifecycle()
    val activeDownloads by nowPlayingViewModel.activeDownloads.collectAsStateWithLifecycle()
    val appNavigationViewModel: AppNavigationViewModel = hiltViewModel()
    // currentBackStackEntryAsState() only reflects the *settled* (post-transition) entry - with
    // the Player route's dismiss transition below taking a couple hundred ms, that would leave
    // the mini-player/tab bar missing (or briefly showing) underneath while it plays. A
    // destination-changed listener fires immediately, independent of how long that takes.
    var currentRoute by remember { mutableStateOf(navController.currentDestination?.route) }
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentRoute = destination.route
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    val pagerState = rememberPagerState(
        initialPage = appNavigationViewModel.lastTabIndex.coerceIn(0, 2),
    ) { 3 }

    LaunchedEffect(Unit) {
        val restoreRoute = appNavigationViewModel.lastRoute
        if (restoreRoute != null && restoreRoute != Routes.MAIN && restoreRoute in RESTORABLE_ROUTES) {
            navController.navigate(restoreRoute)
        }
    }
    LaunchedEffect(currentRoute) {
        val route = currentRoute
        if (route != null && route in RESTORABLE_ROUTES) {
            appNavigationViewModel.rememberRoute(route)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        appNavigationViewModel.rememberTabIndex(pagerState.currentPage)
    }

    Scaffold(
        // Each inner screen's own Scaffold already handles the top status-bar inset, and
        // MiniPlayerBar handles the bottom nav-bar inset itself (navigationBarsPadding) - without
        // this, Scaffold's default systemBars inset reserves that bottom space *again* on top of
        // the bar's own (now taller) measured height, leaving a blank gap above the bar.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Mini-player above, Home/Search/Library tab bar below it (only on the MAIN shell) -
            // the request was explicitly for the tabs to sit below the play bar, not replace it.
            Column {
                // Hidden on the Player screen itself (any episode) the same way MiniPlayerBar is -
                // once you're on Player, that screen's own PreprocessingView already shows this
                // episode's progress in full, so the slim banner would just duplicate it.
                if (activeDownloads.isNotEmpty() && currentRoute != Routes.PLAYER) {
                    DownloadingBanner(
                        downloads = activeDownloads,
                        onClick = { episodeId ->
                            navController.navigate(Routes.player(episodeId)) { launchSingleTop = true }
                        },
                    )
                }
                if (nowPlaying != null && currentRoute != Routes.PLAYER) {
                    MiniPlayerBar(
                        nowPlaying = nowPlaying!!,
                        onClick = {
                            navController.navigate(Routes.player(nowPlaying!!.episodeId)) { launchSingleTop = true }
                        },
                        onTogglePlayPause = nowPlayingViewModel::togglePlayPause,
                    )
                }
                if (currentRoute == Routes.MAIN) {
                    MainTabBar(pagerState = pagerState)
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.MAIN) {
                MainScreen(
                    pagerState = pagerState,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                    onOpenEpisode = { episodeId ->
                        navController.navigate(Routes.player(episodeId)) { launchSingleTop = true }
                    },
                    onPodcastAdded = { podcastId ->
                        navController.navigate(Routes.episodes(podcastId)) { launchSingleTop = true }
                    },
                    onOpenPodcast = { podcastId ->
                        navController.navigate(Routes.episodes(podcastId)) { launchSingleTop = true }
                    },
                    onOpenPlaylist = { playlistId ->
                        navController.navigate(Routes.playlistDetail(playlistId)) { launchSingleTop = true }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUnknownWords = { navController.navigate(Routes.UNKNOWN_WORDS) { launchSingleTop = true } },
                )
            }
            composable(Routes.UNKNOWN_WORDS) {
                UnknownWordsScreen(onBack = { navController.popBackStack() })
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
                // The screen underneath (already fully composed - its own enter transition stays
                // default/instant) is revealed as Player slides down and off; PlayerScreen's own
                // swipe-down-dismiss drag hands off to this same transition mid-flight (see its
                // onDragEnd) so the two motions read as one continuous slide, not two animations.
                popExitTransition = {
                    slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight }
                },
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
