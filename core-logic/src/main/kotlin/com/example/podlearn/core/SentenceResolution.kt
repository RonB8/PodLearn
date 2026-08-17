package com.example.podlearn.core

sealed interface SentenceResolution {
    data class Resolved(val sentenceId: String) : SentenceResolution
    data object NoRelevantSentence : SentenceResolution
}
