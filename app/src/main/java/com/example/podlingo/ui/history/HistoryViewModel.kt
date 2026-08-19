package com.example.podlingo.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.dao.RecentlyPlayedItem
import com.example.podlingo.data.repository.PodcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    podcastRepository: PodcastRepository,
) : ViewModel() {

    val recentlyPlayed: StateFlow<List<RecentlyPlayedItem>> = podcastRepository.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
