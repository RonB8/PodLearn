package com.example.podlearn.core

/**
 * Resolves a pause->quick-resume trigger to the sentence the user actually meant.
 *
 * Algorithm (spec section 4):
 * 1. Shift the raw pause time back by [reactionDelayMs] to account for the natural lag
 *    between hearing something confusing and pressing pause.
 * 2. Binary search [words] (must be sorted ascending by startMs) for the last word whose
 *    startMs <= effective time. That word's sentence is the resolution - we deliberately never
 *    resolve forward to a word that had not started playing yet at the effective time, since the
 *    user cannot have been confused by something they had not heard.
 * 3. If the effective time falls in a silence gap of more than [maxGapMs] since that word ended
 *    (e.g. an ad break), don't guess - report [SentenceResolution.NoRelevantSentence].
 */
object SentenceResolver {

    private const val DEFAULT_MAX_GAP_MS = 4500L

    fun resolve(
        pauseTimeMs: Long,
        reactionDelayMs: Long,
        words: List<WordTiming>,
        maxGapMs: Long = DEFAULT_MAX_GAP_MS,
    ): SentenceResolution {
        if (words.isEmpty()) return SentenceResolution.NoRelevantSentence

        val effectiveTimeMs = pauseTimeMs - reactionDelayMs
        val idx = lastIndexWithStartAtOrBefore(words, effectiveTimeMs)

        if (idx < 0) {
            // Effective time is before every known word (e.g. reaction delay pulled it into the
            // intro silence). Fall back to the first word if it's close enough.
            val gapToFirstWord = words[0].startMs - effectiveTimeMs
            return if (gapToFirstWord <= maxGapMs) {
                SentenceResolution.Resolved(words[0].sentenceId)
            } else {
                SentenceResolution.NoRelevantSentence
            }
        }

        val candidate = words[idx]
        val silenceSinceCandidateEnded = (effectiveTimeMs - candidate.endMs).coerceAtLeast(0)
        return if (silenceSinceCandidateEnded > maxGapMs) {
            SentenceResolution.NoRelevantSentence
        } else {
            SentenceResolution.Resolved(candidate.sentenceId)
        }
    }

    /** Returns the index of the last element with startMs <= [targetMs], or -1 if none exists. */
    private fun lastIndexWithStartAtOrBefore(words: List<WordTiming>, targetMs: Long): Int {
        var low = 0
        var high = words.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (words[mid].startMs <= targetMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
