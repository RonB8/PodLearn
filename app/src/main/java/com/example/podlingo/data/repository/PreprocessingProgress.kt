package com.example.podlingo.data.repository

sealed interface PreprocessingProgress {
    /** [fraction] is 0f..1f, or -1f if the server didn't report a content length. */
    data class Downloading(val fraction: Float) : PreprocessingProgress
    data object Transcribing : PreprocessingProgress
    data object Processing : PreprocessingProgress
    data object Ready : PreprocessingProgress
    data class Failed(val message: String) : PreprocessingProgress
}
