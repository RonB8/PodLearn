package com.example.podlingo.ui.addpodcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AddPodcastUiState {
    data object Idle : AddPodcastUiState
    data object Loading : AddPodcastUiState
    data class Success(val podcastId: String) : AddPodcastUiState
    data class Error(val message: String) : AddPodcastUiState
}

@HiltViewModel
class AddPodcastViewModel @Inject constructor(
    private val repository: PodcastRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddPodcastUiState>(AddPodcastUiState.Idle)
    val uiState: StateFlow<AddPodcastUiState> = _uiState.asStateFlow()

    fun addPodcast(feedUrl: String) {
        val trimmed = feedUrl.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = AddPodcastUiState.Error("Enter an RSS feed URL")
            return
        }
        viewModelScope.launch {
            _uiState.value = AddPodcastUiState.Loading
            _uiState.value = repository.addPodcastByRssUrl(trimmed).fold(
                onSuccess = { AddPodcastUiState.Success(it.id) },
                onFailure = { AddPodcastUiState.Error(it.message ?: "Failed to add podcast") },
            )
        }
    }
}
