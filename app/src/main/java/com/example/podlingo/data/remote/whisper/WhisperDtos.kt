package com.example.podlingo.data.remote.whisper

import kotlinx.serialization.Serializable

/** Shape of the OpenAI `audio/transcriptions` verbose_json response with word timestamps. */
@Serializable
data class WhisperTranscriptionResponse(
    val text: String = "",
    val segments: List<WhisperSegment> = emptyList(),
    val words: List<WhisperWord> = emptyList(),
)

@Serializable
data class WhisperSegment(
    val id: Int = 0,
    val start: Double,
    val end: Double,
    val text: String,
)

@Serializable
data class WhisperWord(
    val word: String,
    val start: Double,
    val end: Double,
)

/** Shape of OpenAI's standard error response body, used to surface a readable failure message. */
@Serializable
data class WhisperErrorResponse(val error: WhisperErrorDetail? = null)

@Serializable
data class WhisperErrorDetail(val message: String? = null, val type: String? = null, val code: String? = null)
