package com.example.podlingo.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.dao.PlaylistWithCount
import com.example.podlingo.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the "add to playlist" dialog, opened from the Player screen. Not route-scoped - [episodeId]
 * is passed per-call rather than read from a nav arg, since this is a modal shown from within an
 * existing screen, not a destination of its own.
 */
@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistWithCount>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun membershipFlow(episodeId: String): Flow<Set<String>> =
        playlistRepository.getPlaylistIdsForEpisode(episodeId).map { it.toSet() }

    fun toggleMembership(playlistId: String, episodeId: String, currentlyIn: Boolean) {
        viewModelScope.launch {
            if (currentlyIn) playlistRepository.removeEpisode(playlistId, episodeId) else playlistRepository.addEpisode(playlistId, episodeId)
        }
    }

    fun createPlaylistAndAdd(name: String, episodeId: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addEpisode(playlistId, episodeId)
        }
    }
}
