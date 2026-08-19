package com.example.podlingo.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.dao.PlaylistWithCount
import com.example.podlingo.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistWithCount>> = playlistRepository.getPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.createPlaylist(name) }
    }

    fun renamePlaylist(playlistId: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.renamePlaylist(playlistId, name) }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { playlistRepository.deletePlaylist(playlistId) }
    }
}
