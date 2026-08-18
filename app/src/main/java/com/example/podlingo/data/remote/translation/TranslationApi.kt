package com.example.podlingo.data.remote.translation

import retrofit2.http.Body
import retrofit2.http.POST

interface TranslationApi {

    /** Calls OpenAI's `POST /v1/chat/completions` to translate a sentence to Hebrew. */
    @POST("chat/completions")
    suspend fun translate(@Body request: ChatCompletionRequest): ChatCompletionResponse
}
