package com.example.podlearn.speech

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
 * Wraps Android's on-device TextToSpeech engine to read translated trigger sentences aloud in
 * Hebrew. App-wide singleton (the engine is expensive to spin up) rather than per-screen.
 * [speak] suspends until the engine reports the utterance finished (or [stop] interrupts it), so
 * the trigger flow can hold episode playback paused for exactly as long as the narration takes.
 */
@Singleton
class HebrewSpeaker @Inject constructor(@ApplicationContext context: Context) {

    private val ready = CompletableDeferred<Boolean>()
    private var pendingContinuation: CancellableContinuation<Unit>? = null

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.forLanguageTag("he-IL")
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
     * Speaks [text] in Hebrew and suspends until the utterance finishes, fails, or [stop] cuts
     * it short. No-ops (returns immediately) if the engine never finished initializing.
     */
    suspend fun speak(text: String) {
        if (!ready.await()) return
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
}
