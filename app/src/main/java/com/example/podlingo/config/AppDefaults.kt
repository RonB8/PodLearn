package com.example.podlingo.config

/**
 * MVP-stage constants for the trigger/resolution pipeline (spec sections 3-4). These are
 * deliberately plain constants, not a DataStore-backed settings screen - per-user calibration
 * and a settings UI belong to the translation-layer follow-up, not this vertical slice.
 */
object AppDefaults {
    const val PAUSE_THRESHOLD_MS = 2000L
    const val REACTION_DELAY_MS = 700L

    /**
     * Extra grace beyond [REACTION_DELAY_MS], on top of the reaction-delay shift SentenceResolver
     * already applies: if the pause still lands within this long of the resolved sentence's own
     * start, the user is almost certainly still reacting to the sentence before it (e.g. only one
     * word of the new sentence has played), so resolve to that previous sentence instead.
     */
    const val SENTENCE_START_GRACE_MS = 700L
    const val SEEK_STEP_MS = 15_000L

    /**
     * Upper bound on how long we hold episode playback paused waiting for the trigger's Hebrew
     * narration to finish. Some OEM TTS engines occasionally drop the utterance-done callback
     * (observed on-device); without this, a dropped callback would pause the episode forever.
     */
    const val TTS_WAIT_TIMEOUT_MS = 15_000L

    /** Oxford CEFR rank (see [com.example.podlingo.data.repository.WordDifficultyRepository]) at or above which a word counts as "hard" - B2, C1, or unranked. */
    const val HARD_WORD_RANK_THRESHOLD = 3

    /** In hard-word mode, a sentence with at least this many hard words gets translated whole instead of one word at a time - past this density a single word stops being enough context. */
    const val AUTO_FULL_SENTENCE_HARD_WORD_COUNT = 3

    /**
     * In hard-word mode, if fewer than this many words of the resolved sentence have been heard
     * by the trigger's effective time, the tail of the *previous* sentence is pulled into the
     * ranking pool too - otherwise a trigger landing right at a sentence boundary only has one or
     * two (often trivial) words of the new sentence to choose from, even though what the user
     * actually just heard was mostly the end of the last one.
     */
    const val MIN_HEARD_WORDS_BEFORE_SENTENCE_LOOKBACK = 2

    /** In vocabulary calibration, marking at least this fraction of a tier's words "don't know" cascades to the next-easier tier. */
    const val CALIBRATION_CASCADE_THRESHOLD = 0.9

    /** Multiple-choice options per end-of-episode quiz question (1 correct + this-1 distractors). */
    const val QUIZ_OPTION_COUNT = 4

    /** Live translation lookups per batch when building a tier of quiz questions - bounds how many concurrent OpenAI calls a single Word Check tier can fire off at once. */
    const val TRANSLATION_FETCH_CONCURRENCY = 8

    /** Downloaded-episode storage cap, user-adjustable in Settings between these bounds - past the limit, episodes are deleted least-recently-played first (see EpisodeStorageManager). */
    const val MIN_STORAGE_LIMIT_BYTES = 500L * 1024 * 1024
    const val MAX_STORAGE_LIMIT_BYTES = 10L * 1024 * 1024 * 1024
    const val DEFAULT_STORAGE_LIMIT_BYTES = 2L * 1024 * 1024 * 1024

    /** How long the start quiz holds each answer's right/wrong reveal before auto-advancing - long enough to register, short enough to stay quick. */
    const val START_QUIZ_AUTO_ADVANCE_DELAY_MS = 900L
}
