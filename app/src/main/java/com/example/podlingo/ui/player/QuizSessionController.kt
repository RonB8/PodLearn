package com.example.podlingo.ui.player

import com.example.podlingo.data.repository.WordKnowledgeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The on-demand vocabulary quiz's state machine ("Quiz" menu item on an episode row) - one
 * instance per owning ViewModel (unscoped, so each screen gets its own quiz session rather than
 * sharing one across the app). Pulled out of [com.example.podlingo.ui.history.HistoryViewModel]
 * so every other episode-list screen (Saved, a podcast's episode list, a playlist) can offer the
 * same "Quiz" menu item without re-implementing this state machine.
 */
class QuizSessionController @Inject constructor(
    private val vocabQuizBuilder: VocabQuizBuilder,
    private val wordKnowledgeRepository: WordKnowledgeRepository,
) {
    private val _quiz = MutableStateFlow<VocabQuizState?>(null)
    val quiz: StateFlow<VocabQuizState?> = _quiz.asStateFlow()

    /** One-shot: Quiz was tapped for an episode with nothing to quiz on yet (never transcribed, or nothing unknown left). */
    private val _noUnknownWordsEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val noUnknownWordsEvent: SharedFlow<Unit> = _noUnknownWordsEvent.asSharedFlow()

    suspend fun start(episodeId: String) {
        val words = vocabQuizBuilder.unknownWordsInEpisode(episodeId)
        val questions = if (words.isEmpty()) emptyList() else vocabQuizBuilder.buildQuizQuestions(words)
        if (questions.isEmpty()) {
            _noUnknownWordsEvent.tryEmit(Unit)
            return
        }
        _quiz.value = VocabQuizState(questions = questions)
    }

    suspend fun onAnswerSelected(answer: String) {
        val current = _quiz.value ?: return
        if (current.answeredThisQuestion != null) return
        val question = current.questions.getOrNull(current.currentIndex) ?: return
        val isCorrect = answer == question.correctAnswer
        if (isCorrect) wordKnowledgeRepository.markKnown(question.word)
        _quiz.update { state ->
            state?.copy(
                answeredThisQuestion = answer,
                correctCount = state.correctCount + if (isCorrect) 1 else 0,
            )
        }
    }

    fun onNext() {
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

    fun onDismissed() {
        _quiz.value = null
    }
}
