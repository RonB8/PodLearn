package com.example.podlingo.di

import javax.inject.Qualifier

/** OkHttpClient configured with the OpenAI auth header and long timeouts for STT calls. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WhisperOkHttpClient
