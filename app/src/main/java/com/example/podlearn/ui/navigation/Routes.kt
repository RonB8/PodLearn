package com.example.podlearn.ui.navigation

object Routes {
    const val PODCASTS = "podcasts"
    const val ADD_PODCAST = "addPodcast"
    const val EPISODES = "episodes/{podcastId}"
    const val PLAYER = "player/{episodeId}"
    const val SETTINGS = "settings"

    fun episodes(podcastId: String) = "episodes/$podcastId"
    fun player(episodeId: String) = "player/$episodeId"
}
