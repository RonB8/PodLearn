package com.example.podlingo.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.dao.RecentlyPlayedItem
import com.example.podlingo.data.local.dao.RecentlyPlayedPodcast
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.ui.player.QuizSessionController
import com.example.podlingo.ui.player.VocabQuizState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val quizSessionController: QuizSessionController,
) : ViewModel() {

    val recentlyPlayed: StateFlow<List<RecentlyPlayedItem>> = podcastRepository.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentlyPlayedPodcasts: StateFlow<List<RecentlyPlayedPodcast>> = podcastRepository.getRecentlyPlayedPodcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Re-fetches every subscribed podcast's RSS feed in parallel for new episodes - existing episodes (downloads, transcripts, play history) are untouched. */
    fun refreshAll() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            coroutineScope {
                podcastRepository.getPodcasts().first()
                    .map { podcast -> async { podcastRepository.addPodcastByRssUrl(podcast.feedUrl) } }
                    .awaitAll()
            }
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

    fun removeFromHistory(episodeId: String) {
        viewModelScope.launch { podcastRepository.removeFromHistory(episodeId) }
    }
}
