package com.example.podlingo.ui.navigation

object Routes {
    /** The Home/Search/Library tabbed shell - the app's start destination. */
    const val MAIN = "main"
    const val EPISODES = "episodes/{podcastId}"
    const val PLAYER = "player/{episodeId}"
    const val SETTINGS = "settings"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"
    const val UNKNOWN_WORDS = "unknown_words"
    const val KNOWN_WORDS = "known_words"

    fun episodes(podcastId: String) = "episodes/$podcastId"
    fun player(episodeId: String) = "player/$episodeId"
    fun playlistDetail(playlistId: String) = "playlist/$playlistId"
}
