package com.example.podlingo.ui.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.dao.PlaylistEpisodeItem
import com.example.podlingo.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])

    val playlistName: StateFlow<String> = playlistRepository.getPlaylist(playlistId)
        .map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val episodes: StateFlow<List<PlaylistEpisodeItem>> = playlistRepository.getEpisodes(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun removeEpisode(episodeId: String) {
        viewModelScope.launch { playlistRepository.removeEpisode(playlistId, episodeId) }
    }
}
