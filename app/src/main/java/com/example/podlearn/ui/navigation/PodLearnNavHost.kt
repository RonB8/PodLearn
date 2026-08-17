package com.example.podlearn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.podlearn.ui.addpodcast.AddPodcastScreen
import com.example.podlearn.ui.episodelist.EpisodeListScreen
import com.example.podlearn.ui.player.PlayerScreen
import com.example.podlearn.ui.podcastlist.PodcastListScreen
import com.example.podlearn.ui.settings.SettingsScreen

@Composable
fun PodLearnNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.PODCASTS) {
        composable(Routes.PODCASTS) {
            PodcastListScreen(
                onAddPodcast = { navController.navigate(Routes.ADD_PODCAST) { launchSingleTop = true } },
                onOpenPodcast = { podcastId ->
                    navController.navigate(Routes.episodes(podcastId)) { launchSingleTop = true }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) { launchSingleTop = true } },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
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
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}
