package com.example.podlearn.di

import com.example.podlearn.BuildConfig
import com.example.podlearn.data.remote.translation.TranslationApi
import com.example.podlearn.data.remote.whisper.WhisperApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val OPENAI_BASE_URL = "https://api.openai.com/v1/"

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    /** Plain client used for RSS feed fetches and mp3 downloads - no auth needed. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /** Client used for Whisper calls: carries the OpenAI auth header and allows for long uploads/transcriptions. */
    @Provides
    @Singleton
    @WhisperOkHttpClient
    fun provideWhisperOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .addInterceptor { chain ->
            val authorized = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .build()
            chain.proceed(authorized)
        }
        .build()

    @Provides
    @Singleton
    fun provideWhisperApi(@WhisperOkHttpClient client: OkHttpClient, json: Json): WhisperApi =
        Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WhisperApi::class.java)

    /** Chat completions are used for Hebrew translation of trigger sentences; shares the OpenAI auth client. */
    @Provides
    @Singleton
    fun provideTranslationApi(@WhisperOkHttpClient client: OkHttpClient, json: Json): TranslationApi =
        Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TranslationApi::class.java)
}
