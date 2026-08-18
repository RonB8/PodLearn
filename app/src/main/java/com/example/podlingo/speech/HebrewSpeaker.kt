package com.example.podlingo.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps Android's on-device TextToSpeech engine to read trigger words/sentences aloud - Hebrew
 * translations by default, or the original English via the [ENGLISH] locale (used to read the
 * source word before its translation in hard-word mode). App-wide singleton (the engine is
 * expensive to spin up) rather than per-screen. [speak] suspends until the engine reports the
 * utterance finished (or [stop] interrupts it), so the trigger flow can hold episode playback
 * paused for exactly as long as the narration takes.
 */
@Singleton
class HebrewSpeaker @Inject constructor(@ApplicationContext context: Context) {

    private val ready = CompletableDeferred<Boolean>()
    private var pendingContinuation: CancellableContinuation<Unit>? = null

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = HEBREW
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = resumePending()

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = resumePending()
                    override fun onError(utteranceId: String?, errorCode: Int) = resumePending()
                })
            }
            ready.complete(status == TextToSpeech.SUCCESS)
        }
    }

    /**
     * Speaks [text] in [locale] (Hebrew by default) and suspends until the utterance finishes,
     * fails, or [stop] cuts it short. No-ops (returns immediately) if the engine never finished
     * initializing.
     */
    suspend fun speak(text: String, locale: Locale = HEBREW) {
        if (!ready.await()) return
        tts.language = locale
        suspendCancellableCoroutine<Unit> { cont ->
            pendingContinuation = cont
            val queued = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
            if (queued == TextToSpeech.ERROR) {
                resumePending()
            }
            cont.invokeOnCancellation {
                pendingContinuation = null
                tts.stop()
            }
        }
    }

    /** Cuts off any speech in progress and unblocks a suspended [speak] call immediately. */
    fun stop() {
        if (::tts.isInitialized) tts.stop()
        resumePending()
    }

    private fun resumePending() {
        pendingContinuation?.let { if (it.isActive) it.resume(Unit) }
        pendingContinuation = null
    }

    companion object {
        val HEBREW: Locale = Locale.forLanguageTag("he-IL")
        val ENGLISH: Locale = Locale.forLanguageTag("en-US")
    }
}
