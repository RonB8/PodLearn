package com.example.podlingo.core

sealed interface SentenceResolution {
    data class Resolved(val sentenceId: String) : SentenceResolution
    data object NoRelevantSentence : SentenceResolution
}
