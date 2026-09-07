package com.example.podlingo.di

import javax.inject.Qualifier

/** OkHttpClient configured with the OpenAI auth header and long timeouts for STT calls. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WhisperOkHttpClient

/**
 * OkHttpClient configured with the OpenAI auth header and short timeouts for chat-completion
 * translation calls - deliberately separate from [WhisperOkHttpClient], whose multi-minute
 * timeouts (sized for large audio uploads) would otherwise leave a stuck translation request
 * blocking playback resume for minutes instead of failing fast.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TranslationOkHttpClient
