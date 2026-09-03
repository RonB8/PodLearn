package com.example.podlingo.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.dao.RecentlyPlayedItem
import com.example.podlingo.data.local.dao.RecentlyPlayedPodcast
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.data.repository.WordKnowledgeRepository
import com.example.podlingo.ui.player.VocabQuizBuilder
import com.example.podlingo.ui.player.VocabQuizState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val wordKnowledgeRepository: WordKnowledgeRepository,
    private val vocabQuizBuilder: VocabQuizBuilder,
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

    /** Non-null while the on-demand quiz (Home tab's "Quiz" menu item) is open for some episode. */
    private val _quiz = MutableStateFlow<VocabQuizState?>(null)
    val quiz: StateFlow<VocabQuizState?> = _quiz.asStateFlow()

    /** One-shot: Quiz was tapped for an episode with nothing to quiz on yet. */
    private val _noUnknownWordsEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val noUnknownWordsEvent: SharedFlow<Unit> = _noUnknownWordsEvent.asSharedFlow()

    fun startQuiz(episodeId: String) {
        viewModelScope.launch {
            val words = vocabQuizBuilder.unknownWordsInEpisode(episodeId)
            val questions = if (words.isEmpty()) emptyList() else vocabQuizBuilder.buildQuizQuestions(words)
            if (questions.isEmpty()) {
                _noUnknownWordsEvent.tryEmit(Unit)
                return@launch
            }
            _quiz.value = VocabQuizState(questions = questions)
        }
    }

    fun onQuizAnswerSelected(answer: String) {
        val current = _quiz.value ?: return
        if (current.answeredThisQuestion != null) return
        val question = current.questions.getOrNull(current.currentIndex) ?: return
        val isCorrect = answer == question.correctAnswer
        viewModelScope.launch {
            if (isCorrect) wordKnowledgeRepository.markKnown(question.word)
            _quiz.update { state ->
                state?.copy(
                    answeredThisQuestion = answer,
                    correctCount = state.correctCount + if (isCorrect) 1 else 0,
                )
            }
        }
    }

    fun onQuizNext() {
        _quiz.update { state ->
            if (state == null) return@update state
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.questions.size) {
                state.copy(finished = true)
            } else {
                state.copy(currentIndex = nextIndex, answeredThisQuestion = null)
            }
        }
    }

    fun onQuizDismissed() {
        _quiz.value = null
    }

    fun removeFromHistory(episodeId: String) {
        viewModelScope.launch { podcastRepository.removeFromHistory(episodeId) }
    }
}
