package com.example.podlingo.di

import com.example.podlingo.BuildConfig
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchApi
import com.example.podlingo.data.remote.translation.TranslationApi
import com.example.podlingo.data.remote.whisper.WhisperApi
import com.example.podlingo.data.repository.PodcastSearchRepository
import com.example.podlingo.data.repository.PodcastSearchRepositoryImpl
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
    private const val ITUNES_BASE_URL = "https://itunes.apple.com/"

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

    /**
     * Exposes the plain client via [Call.Factory] rather than [OkHttpClient] for consumers (like
     * [com.example.podlingo.data.repository.PodcastRepository]) that only need to place calls -
     * lets tests substitute a fake factory without spinning up a real HTTP client.
     */
    @Provides
    @Singleton
    fun provideCallFactory(client: OkHttpClient): okhttp3.Call.Factory = client

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

    /**
     * Client used for chat-completion translation calls: same OpenAI auth as [provideWhisperOkHttpClient],
     * but short timeouts - a translation request is a few words of text, not an audio upload, so it
     * should fail fast rather than potentially blocking playback resume (auto-translate pauses
     * playback until the translation comes back) for minutes.
     */
    @Provides
    @Singleton
    @TranslationOkHttpClient
    fun provideTranslationOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val authorized = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .build()
            chain.proceed(authorized)
        }
        .build()

    /** Chat completions are used for Hebrew translation of trigger sentences. */
    @Provides
    @Singleton
    fun provideTranslationApi(@TranslationOkHttpClient client: OkHttpClient, json: Json): TranslationApi =
        Retrofit.Builder()
            .baseUrl(OPENAI_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TranslationApi::class.java)

    /** iTunes's search endpoint needs no auth; shares the plain client used for RSS feed fetches. */
    @Provides
    @Singleton
    fun providePodcastSearchApi(client: OkHttpClient, json: Json): PodcastSearchApi =
        Retrofit.Builder()
            .baseUrl(ITUNES_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PodcastSearchApi::class.java)

    @Provides
    @Singleton
    fun providePodcastSearchRepository(api: PodcastSearchApi): PodcastSearchRepository =
        PodcastSearchRepositoryImpl(api)
}
