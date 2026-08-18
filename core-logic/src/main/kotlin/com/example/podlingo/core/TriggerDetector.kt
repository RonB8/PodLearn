package com.example.podlingo.core

sealed interface TriggerResult {
    data class Triggered(val pauseTimeMs: Long) : TriggerResult
    data object NotTriggered : TriggerResult
}

/**
 * Detects the "pause, then quick resume" gesture (spec section 3).
 *
 * Not thread-safe; callers should confine an instance to a single playback controller.
 */
class TriggerDetector(private val thresholdMs: Long = DEFAULT_THRESHOLD_MS) {

    private var lastPauseWallClockMs: Long? = null
    private var lastPauseMediaPositionMs: Long? = null

    /** Record a pause event. [wallClockMs] and [mediaPositionMs] use any consistent clock. */
    fun onPause(wallClockMs: Long, mediaPositionMs: Long) {
        lastPauseWallClockMs = wallClockMs
        lastPauseMediaPositionMs = mediaPositionMs
    }

    /**
     * Record a resume event. Returns [TriggerResult.Triggered] with the media position at the
     * moment of the preceding pause if resume happened within [thresholdMs] of it.
     */
    fun onResume(wallClockMs: Long): TriggerResult {
        val pauseAt = lastPauseWallClockMs
        val pauseMediaPosition = lastPauseMediaPositionMs
        lastPauseWallClockMs = null
        lastPauseMediaPositionMs = null

        if (pauseAt == null || pauseMediaPosition == null) return TriggerResult.NotTriggered

        return if (wallClockMs - pauseAt <= thresholdMs) {
            TriggerResult.Triggered(pauseTimeMs = pauseMediaPosition)
        } else {
            TriggerResult.NotTriggered
        }
    }

    companion object {
        const val DEFAULT_THRESHOLD_MS = 2000L
    }
}
