package com.example.podlearn.config

/**
 * MVP-stage constants for the trigger/resolution pipeline (spec sections 3-4). These are
 * deliberately plain constants, not a DataStore-backed settings screen - per-user calibration
 * and a settings UI belong to the translation-layer follow-up, not this vertical slice.
 */
object AppDefaults {
    const val PAUSE_THRESHOLD_MS = 2000L
    const val REACTION_DELAY_MS = 700L
    const val SEEK_STEP_MS = 15_000L

    /**
     * Upper bound on how long we hold episode playback paused waiting for the trigger's Hebrew
     * narration to finish. Some OEM TTS engines occasionally drop the utterance-done callback
     * (observed on-device); without this, a dropped callback would pause the episode forever.
     */
    const val TTS_WAIT_TIMEOUT_MS = 15_000L
}
