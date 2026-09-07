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
        /** Mirrors [com.example.podlingo.data.repository.SettingsRepository.hardWordModeTriggerEnabled] - the trigger flavor only; there's no Player-screen control for either flavor any more, both live in Settings, but the trigger overlay logic below still needs to react to it. */
        val hardWordModeTriggerEnabled: Boolean = false,
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
        /** A transient inline translation shown while an unknown word plays - see [PlayerViewModel]. */
        val translationPopup: WordTranslationPopup? = null,
        /** True while the "review words you didn't know?" Yes/No prompt is showing after the episode ends. */
        val quizPrompt: Boolean = false,
        /** Non-null while the end-of-episode vocabulary quiz (offered after [quizPrompt]) is active. */
        val quiz: VocabQuizState? = null,
        /** Non-null while the pre-episode "Word Check" assessment is open - see [PlayerViewModel.beginWordCheck]. Opens directly (no Yes/No gate) on a genuinely fresh start once there's anything left to assess. */
        val wordCheck: WordCheckState? = null,
        /** True while a Word Check tier is being built (translations fetched, questions assembled) but isn't ready to show yet - a large first tier can take a few seconds, and without this the screen would otherwise look frozen (no audio playing, no dialog yet). */
        val wordCheckLoading: Boolean = false,
    ) : PlayerScreenState

    data class Failed(val message: String, val episodeTitle: String? = null) : PlayerScreenState
}
