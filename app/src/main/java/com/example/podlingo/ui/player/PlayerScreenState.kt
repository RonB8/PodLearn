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
    ) : PlayerScreenState

    data class Failed(val message: String, val episodeTitle: String? = null) : PlayerScreenState
}
