package com.example.podlingo.ui.player

import com.example.podlingo.config.AppDefaults
import com.example.podlingo.core.WordNormalizer
import com.example.podlingo.data.local.entity.WordKnowledgeStatus
import com.example.podlingo.data.repository.TranscriptRepository
import com.example.podlingo.data.repository.WordKnowledgeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the end-of-episode vocabulary quiz for a given episode - shared between [PlayerViewModel]
 * (offered right after an episode finishes playing) and the Home tab's per-episode "Quiz" menu item
 * (same quiz, built on demand for any already-transcribed episode in history).
 */
@Singleton
class VocabQuizBuilder @Inject constructor(
    private val transcriptRepository: TranscriptRepository,
    private val wordKnowledgeRepository: WordKnowledgeRepository,
) {
    suspend fun unknownWordsInEpisode(episodeId: String): List<String> {
        val words = transcriptRepository.getWordTimings(episodeId)
        val deduped = words.distinctBy { WordNormalizer.normalize(it.word) }
        val statuses = wordKnowledgeRepository.getStatuses(deduped.map { it.word })
        return deduped
            .filter { statuses[WordNormalizer.normalize(it.word)]?.status == WordKnowledgeStatus.UNKNOWN }
            .map { it.word }
    }

    /** One question per word: the real cached/fetched translation plus 3 distractors sampled from other real translations - never an LLM call. */
    suspend fun buildQuizQuestions(words: List<String>): List<VocabQuizQuestion> {
        val questions = mutableListOf<VocabQuizQuestion>()
        for (word in words) {
            val correct = wordKnowledgeRepository.getOrFetchTranslation(word) ?: continue
            val distractorCount = AppDefaults.QUIZ_OPTION_COUNT - 1
            val distractors = wordKnowledgeRepository.sampleDistractors(word, distractorCount).toMutableSet()
            if (distractors.size < distractorCount) {
                val fallback = questions.map { it.correctAnswer }.filter { it != correct && it !in distractors }
                distractors += fallback.shuffled().take(distractorCount - distractors.size)
            }
            // Not enough real wrong answers exist anywhere yet (e.g. this is the very first
            // quiz ever) - skip rather than show a question with fewer than 4 options.
            if (distractors.size < distractorCount) continue
            questions += VocabQuizQuestion(word = word, correctAnswer = correct, options = (distractors + correct).shuffled())
        }
        return questions
    }
}
