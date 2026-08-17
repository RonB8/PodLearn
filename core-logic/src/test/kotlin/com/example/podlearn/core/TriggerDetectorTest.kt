package com.example.podlearn.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerDetectorTest {

    @Test
    fun `resume within threshold fires a trigger with the media position at pause`() {
        val detector = TriggerDetector(thresholdMs = 2000)
        detector.onPause(wallClockMs = 10_000, mediaPositionMs = 45_000)

        val result = detector.onResume(wallClockMs = 11_500)

        assertEquals(TriggerResult.Triggered(pauseTimeMs = 45_000), result)
    }

    @Test
    fun `resume exactly at the threshold still fires`() {
        val detector = TriggerDetector(thresholdMs = 2000)
        detector.onPause(wallClockMs = 10_000, mediaPositionMs = 45_000)

        val result = detector.onResume(wallClockMs = 12_000)

        assertEquals(TriggerResult.Triggered(pauseTimeMs = 45_000), result)
    }

    @Test
    fun `resume beyond threshold does not fire`() {
        val detector = TriggerDetector(thresholdMs = 2000)
        detector.onPause(wallClockMs = 10_000, mediaPositionMs = 45_000)

        val result = detector.onResume(wallClockMs = 12_001)

        assertEquals(TriggerResult.NotTriggered, result)
    }

    @Test
    fun `resume without a preceding pause does not fire`() {
        val detector = TriggerDetector(thresholdMs = 2000)

        val result = detector.onResume(wallClockMs = 5_000)

        assertEquals(TriggerResult.NotTriggered, result)
    }

    @Test
    fun `a consumed trigger does not fire again on a later resume`() {
        val detector = TriggerDetector(thresholdMs = 2000)
        detector.onPause(wallClockMs = 10_000, mediaPositionMs = 45_000)
        detector.onResume(wallClockMs = 11_000) // consumes the pause

        val secondResult = detector.onResume(wallClockMs = 11_500)

        assertEquals(TriggerResult.NotTriggered, secondResult)
    }

    @Test
    fun `repeated pause-resume cycles are each evaluated independently`() {
        val detector = TriggerDetector(thresholdMs = 2000)

        detector.onPause(wallClockMs = 1_000, mediaPositionMs = 10_000)
        val first = detector.onResume(wallClockMs = 5_000) // too slow
        assertEquals(TriggerResult.NotTriggered, first)

        detector.onPause(wallClockMs = 6_000, mediaPositionMs = 20_000)
        val second = detector.onResume(wallClockMs = 6_500) // quick enough
        assertEquals(TriggerResult.Triggered(pauseTimeMs = 20_000), second)
    }
}
