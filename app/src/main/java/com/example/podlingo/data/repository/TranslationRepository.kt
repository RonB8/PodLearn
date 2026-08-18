package com.example.podlingo.data.repository

import com.example.podlingo.data.remote.translation.ChatCompletionRequest
import com.example.podlingo.data.remote.translation.ChatMessage
import com.example.podlingo.data.remote.translation.TranslationApi
import javax.inject.Inject

/** Translates resolved sentences to Hebrew via OpenAI chat completions for the trigger overlay. */
class TranslationRepository @Inject constructor(
    private val translationApi: TranslationApi,
) {

    suspend fun translateToHebrew(text: String): Result<String> = runCatching {
        val response = translationApi.translate(
            ChatCompletionRequest(
                model = MODEL,
                messages = listOf(
                    ChatMessage(role = "system", content = SYSTEM_PROMPT),
                    ChatMessage(role = "user", content = text),
                ),
            ),
        )
        response.choices.firstOrNull()?.message?.content?.trim()
            ?: error("No translation returned")
    }

    companion object {
        private const val MODEL = "gpt-4o-mini"
        private const val SYSTEM_PROMPT =
            "You are a translator. Translate the user's message from English to Hebrew. " +
                "Respond with only the Hebrew translation, no quotes, no explanations."
    }
}
