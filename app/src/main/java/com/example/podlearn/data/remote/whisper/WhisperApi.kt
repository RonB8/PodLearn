package com.example.podlearn.data.remote.whisper

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface WhisperApi {

    /**
     * Calls OpenAI's `POST /v1/audio/transcriptions` with word- and segment-level timestamps.
     * Both granularities must be requested explicitly - asking for "word" alone omits `segments`
     * from the response entirely, which is what we rely on for sentence boundaries (spec §2.3).
     * Authorization is attached by an interceptor on the Retrofit client's OkHttpClient, not here.
     */
    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribe(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody,
        @Part("timestamp_granularities[]") wordGranularity: RequestBody,
        @Part("timestamp_granularities[]") segmentGranularity: RequestBody,
    ): WhisperTranscriptionResponse
}
