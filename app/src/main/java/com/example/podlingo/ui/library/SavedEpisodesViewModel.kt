package com.example.podlingo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.dao.SavedEpisodeItem
import com.example.podlingo.data.repository.EpisodeStorageManager
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.ui.player.QuizSessionController
import com.example.podlingo.ui.player.VocabQuizState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SavedEpisodesViewModel @Inject constructor(
    repository: PodcastRepository,
    private val episodeStorageManager: EpisodeStorageManager,
    private val quizSessionController: QuizSessionController,
) : ViewModel() {

    val episodes: StateFlow<List<SavedEpisodeItem>> = repository.getSavedEpisodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
