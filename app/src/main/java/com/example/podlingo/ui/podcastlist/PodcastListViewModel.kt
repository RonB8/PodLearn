package com.example.podlingo.ui.podcastlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.entity.PodcastEntity
import com.example.podlingo.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PodcastListViewModel @Inject constructor(
    repository: PodcastRepository,
) : ViewModel() {

    val podcasts: StateFlow<List<PodcastEntity>> = repository.getPodcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
