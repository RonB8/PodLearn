package com.example.podlingo.ui.player

import com.example.podlingo.core.WordTiming
import com.example.podlingo.data.local.entity.SentenceEntity
import com.example.podlingo.data.repository.PreprocessingProgress
import com.example.podlingo.player.PlayerUiState

sealed interface PlayerScreenState {
    data object Loading : PlayerScreenState

    data class Preprocessing(
        val episodeTitle: String,
        val progress: PreprocessingProgress,
    ) : PlayerScreenState

    data class Ready(
        val episodeId: String,
        val episodeTitle: String,
        val player: PlayerUiState,
        val artworkUrl: String? = null,
        val hasNextEpisode: Boolean = false,
        val resolvedSentenceText: String? = null,
        val translatedSentenceText: String? = null,
        val isTranslating: Boolean = false,
        val noRelevantSentence: Boolean = false,
        val sentences: List<SentenceEntity> = emptyList(),
        val words: List<WordTiming> = emptyList(),
        val transcriptVisible: Boolean = false,
        /** Mirrors [com.example.podlingo.data.repository.SettingsRepository.hardWordModeEnabled]. */
        val hardWordModeEnabled: Boolean = false,
        /** The sentence currently being read aloud by a trigger (translation overlay), if any. */
        val activeSentenceId: String? = null,
        /** Hard-word mode's specific target word within [activeSentenceId], if that's the active trigger. */
        val activeWord: String? = null,
        /** Mirrors [com.example.podlingo.data.repository.SettingsRepository.autoTranslateEnabled]. */
        val autoTranslateEnabled: Boolean = false,
        /** Mirrors [com.example.podlingo.data.repository.SettingsRepository.showSentenceTranslationsEnabled]. */
        val showSentenceTranslationsEnabled: Boolean = false,
        /** Hebrew translation per sentence id, filled in lazily as sentences scroll into view - see [PlayerViewModel.ensureSentenceTranslation]. Not persisted; refetched each playthrough, matching the trigger overlay's own translations. */
        val sentenceTranslations: Map<String, String> = emptyMap(),
        /** Sentence ids with a translation fetch in flight, so [PlayerViewModel.ensureSentenceTranslation] never fires the same request twice. */
        val translatingSentenceIds: Set<String> = emptySet(),
        /** Non-null while the "which of these words do you know" panel is open for this episode. */
        val vocabCalibration: VocabCalibrationState? = null,
        /** A transient inline translation shown while an unknown word plays - see [PlayerViewModel]. */
        val translationPopup: WordTranslationPopup? = null,
        /** True while the "review words you didn't know?" Yes/No prompt is showing after the episode ends. */
        val quizPrompt: Boolean = false,
        /** Non-null while the end-of-episode vocabulary quiz, or the pre-episode start quiz, is active - see [VocabQuizState.autoAdvance] to tell which. */
        val quiz: VocabQuizState? = null,
        /** True while the "quiz yourself before you start?" Yes/No prompt is showing - only ever offered once per episode, on a genuinely fresh start (see [PlayerViewModel.startPlayback]). */
        val startQuizPrompt: Boolean = false,
    ) : PlayerScreenState

    data class Failed(val message: String, val episodeTitle: String? = null) : PlayerScreenState
}
