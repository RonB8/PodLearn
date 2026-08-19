package com.example.podlingo.ui.player

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
    ) : PlayerScreenState

    data class Failed(val message: String, val episodeTitle: String? = null) : PlayerScreenState
}
