package com.example.podlingo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NowPlayingUi(
    val episodeId: String,
    val episodeTitle: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

/**
 * Activity-scoped (created once, directly inside [com.example.podlingo.ui.navigation.PodLingoNavHost]
 * rather than any single route) so the persistent mini-player bar reflects [PlayerController]'s
 * shared state no matter which screen is on top.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerController: PlayerController,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlayingUi?> = playerController.playerState
        .map { state ->
            val episodeId = state.episodeId ?: return@map null
            NowPlayingUi(
                episodeId = episodeId,
                episodeTitle = state.episodeTitle.orEmpty(),
                artworkUrl = state.artworkUrl,
                isPlaying = state.isPlaying,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePlayPause() {
        if (playerController.playerState.value.isPlaying) playerController.pause() else playerController.play()
    }
}
