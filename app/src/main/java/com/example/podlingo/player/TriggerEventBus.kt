package com.example.podlingo.player

import com.example.podlingo.core.TriggerResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bridges trigger detection to the UI layer. Detection now happens in [PlaybackService] (see its
 * `MediaSession.Callback`), since it has to react to play/pause requests from *any* source - the
 * in-app button, the system notification, or the lock screen - not just in-app taps. Service and
 * ViewModel run in the same process, so a plain shared flow is enough; no IPC needed.
 */
@Singleton
class TriggerEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<TriggerResult.Triggered>(extraBufferCapacity = 1)
    val events: SharedFlow<TriggerResult.Triggered> = _events.asSharedFlow()

    private val _previousSentenceRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val previousSentenceRequests: SharedFlow<Unit> = _previousSentenceRequests.asSharedFlow()

    /**
     * True while a full-sentence translation is currently being fetched/read aloud - kept in sync
     * by [com.example.podlingo.ui.player.PlayerViewModel] and read synchronously here by
     * [PlaybackService]'s `MediaSession.Callback`, so a play/pause command arriving mid-narration
     * from *any* source (headphones, notification, lock screen, in-app button) is treated the same:
     * as a request for the previous sentence rather than a normal resume.
     */
    @Volatile
    var translationOverlayActive: Boolean = false

    fun emit(trigger: TriggerResult.Triggered) {
        _events.tryEmit(trigger)
    }

    fun requestPreviousSentence() {
        _previousSentenceRequests.tryEmit(Unit)
    }
}
