package com.example.podlingo.ui.episodelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.repository.EpisodeStorageManager
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.ui.player.QuizSessionController
import com.example.podlingo.ui.player.VocabQuizState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PodcastRepository,
    private val episodeStorageManager: EpisodeStorageManager,
    private val quizSessionController: QuizSessionController,
) : ViewModel() {

    private val podcastId: String = checkNotNull(savedStateHandle["podcastId"])

    val episodes: StateFlow<List<EpisodeEntity>> = repository.getEpisodes(podcastId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Re-fetches this podcast's RSS feed for new episodes - existing ones (downloads, transcripts, play history) are untouched, new ones are added to the list already shown. */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val feedUrl = repository.getPodcast(podcastId)?.feedUrl
            if (feedUrl != null) repository.addPodcastByRssUrl(feedUrl)
            _refreshing.value = false
        }
    }

    val quiz: StateFlow<VocabQuizState?> = quizSessionController.quiz
    val noUnknownWordsEvent: SharedFlow<Unit> = quizSessionController.noUnknownWordsEvent

    fun startQuiz(episodeId: String) {
        viewModelScope.launch { quizSessionController.start(episodeId) }
    }

    fun onQuizAnswerSelected(answer: String) {
        viewModelScope.launch { quizSessionController.onAnswerSelected(answer) }
    }

    fun onQuizNext() = quizSessionController.onNext()

    fun onQuizDismissed() = quizSessionController.onDismissed()

    fun removeDownload(episodeId: String) {
        viewModelScope.launch { episodeStorageManager.deleteDownload(episodeId) }
    }
}
