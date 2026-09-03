package com.example.podlingo.ui.player

import com.example.podlingo.data.repository.PreprocessingProgress
import com.example.podlingo.ui.strings.AppStrings

/** Shared between the full-screen Player preprocessing view and the app-wide downloading banner, so the two never drift out of sync. */
fun PreprocessingProgress.label(strings: AppStrings): String = when (this) {
    is PreprocessingProgress.Downloading -> strings.downloadingEpisode
    is PreprocessingProgress.Transcribing -> if (chunkCount > 1) {
        strings.transcribingSpeechPart(chunkIndex, chunkCount)
    } else {
        strings.transcribingSpeech
    }
    PreprocessingProgress.Processing -> strings.buildingTranscript
    PreprocessingProgress.Ready -> strings.ready
    is PreprocessingProgress.Failed -> message
}
