package com.example.podlingo.ui.addpodcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchResult
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.data.repository.PodcastSearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PodcastSearchUiState {
    data object Idle : PodcastSearchUiState
    data object Loading : PodcastSearchUiState
    data class Results(val items: List<PodcastSearchResultItem>) : PodcastSearchUiState
    data object NoResults : PodcastSearchUiState
    data class NetworkError(val message: String) : PodcastSearchUiState
}

data class PodcastSearchResultItem(
    val result: PodcastSearchResult,
    val alreadyAdded: Boolean,
)

sealed interface AddPodcastUiState {
    data object Idle : AddPodcastUiState
    data object Loading : AddPodcastUiState
    data class Success(val podcastId: String) : AddPodcastUiState
    data class Error(val message: String) : AddPodcastUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class AddPodcastViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val searchRepository: PodcastSearchRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _addState = MutableStateFlow<AddPodcastUiState>(AddPodcastUiState.Idle)
    val addState: StateFlow<AddPodcastUiState> = _addState.asStateFlow()

    val searchState: StateFlow<PodcastSearchUiState> = _query
        .debounce(SEARCH_DEBOUNCE_MS)
        .distinctUntilChanged()
        .flatMapLatest { rawQuery -> searchResultsFor(rawQuery.trim()) }
        .combine(podcastRepository.getPodcasts()) { state, addedPodcasts ->
            if (state !is PodcastSearchUiState.Results) return@combine state
            val addedFeedUrls = addedPodcasts.mapTo(HashSet()) { it.feedUrl }
            PodcastSearchUiState.Results(
                state.items.map { it.copy(alreadyAdded = it.result.feedUrl in addedFeedUrls) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PodcastSearchUiState.Idle)

    private fun searchResultsFor(trimmedQuery: String): Flow<PodcastSearchUiState> =
        if (trimmedQuery.isEmpty()) {
            flowOf(PodcastSearchUiState.Idle)
        } else {
            flow {
                emit(PodcastSearchUiState.Loading)
                emit(
                    searchRepository.search(trimmedQuery).fold(
                        onSuccess = { results ->
                            if (results.isEmpty()) {
                                PodcastSearchUiState.NoResults
                            } else {
                                PodcastSearchUiState.Results(
                                    results.map { PodcastSearchResultItem(it, alreadyAdded = false) },
                                )
                            }
                        },
                        onFailure = { PodcastSearchUiState.NetworkError(it.message ?: "Search failed") },
                    ),
                )
            }
        }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
    }

    /**
     * Called once the navigation triggered by a [AddPodcastUiState.Success] has actually
     * happened, so returning to this screen (e.g. via back navigation) doesn't find [addState]
     * still sitting on that same Success value and immediately re-fire the navigation - trapping
     * back navigation in a loop straight back to the episode list it just came from.
     */
    fun consumeAddResult() {
        if (_addState.value is AddPodcastUiState.Success) {
            _addState.value = AddPodcastUiState.Idle
        }
    }

    /**
     * Selecting an already-added result skips the network round-trip entirely (it would just
     * re-fetch and re-upsert the same feed) and resolves straight from the local DB.
     */
    fun selectResult(item: PodcastSearchResultItem) {
        if (item.alreadyAdded) {
            viewModelScope.launch {
                _addState.value = AddPodcastUiState.Loading
                val existing = podcastRepository.getByFeedUrl(item.result.feedUrl)
                _addState.value = if (existing != null) {
                    AddPodcastUiState.Success(existing.id)
                } else {
                    addPodcastByFeedUrl(item.result.feedUrl)
                }
            }
        } else {
            viewModelScope.launch {
                _addState.value = AddPodcastUiState.Loading
                _addState.value = addPodcastByFeedUrl(item.result.feedUrl)
            }
        }
    }

    /** Fallback manual entry path - kept for when search can't find a feed. */
    fun addPodcast(feedUrl: String) {
        val trimmed = feedUrl.trim()
        if (trimmed.isEmpty()) {
            _addState.value = AddPodcastUiState.Error("Enter an RSS feed URL")
            return
        }
        viewModelScope.launch {
            _addState.value = AddPodcastUiState.Loading
            _addState.value = addPodcastByFeedUrl(trimmed)
        }
    }

    private suspend fun addPodcastByFeedUrl(feedUrl: String): AddPodcastUiState =
        podcastRepository.addPodcastByRssUrl(feedUrl).fold(
            onSuccess = { AddPodcastUiState.Success(it.id) },
            onFailure = { AddPodcastUiState.Error(it.message ?: "Failed to add podcast") },
        )

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 400L
    }
}
