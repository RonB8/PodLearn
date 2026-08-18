package com.example.podlingo.ui.episodelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class EpisodeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PodcastRepository,
) : ViewModel() {

    private val podcastId: String = checkNotNull(savedStateHandle["podcastId"])

    val episodes: StateFlow<List<EpisodeEntity>> = repository.getEpisodes(podcastId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
