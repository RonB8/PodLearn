package com.example.podlearn.ui.player

import com.example.podlearn.data.repository.PreprocessingProgress
import com.example.podlearn.player.PlayerUiState

sealed interface PlayerScreenState {
    data object Loading : PlayerScreenState

    data class Preprocessing(
        val episodeTitle: String,
        val progress: PreprocessingProgress,
    ) : PlayerScreenState

    data class Ready(
        val episodeTitle: String,
        val player: PlayerUiState,
        val resolvedSentenceText: String? = null,
        val translatedSentenceText: String? = null,
        val isTranslating: Boolean = false,
        val noRelevantSentence: Boolean = false,
    ) : PlayerScreenState

    data class Failed(val message: String, val episodeTitle: String? = null) : PlayerScreenState
}
