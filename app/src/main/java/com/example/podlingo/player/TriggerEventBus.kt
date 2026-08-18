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

    fun emit(trigger: TriggerResult.Triggered) {
        _events.tryEmit(trigger)
    }
}
