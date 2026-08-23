package com.example.podlingo.core

/**
 * Orders a sentence's words from hardest to easiest, for the progressive hard-word trigger mode:
 * the first trigger on a sentence reveals the hardest word; an immediate re-trigger on the same
 * sentence reveals the next-hardest, and so on (settings toggle).
 */
object WordDifficultyRanker {

    /**
     * [rankOf] returns a word's difficulty rank; lower is easier, and callers should give
     * unrecognized words the highest rank (per the difficulty source's own contract that "not
     * found" means hardest). [isKnownWord] reports whether the word is in the difficulty source's
     * vocabulary at all - see below. Words with no letters (stray punctuation tokens) are dropped.
     * Ties keep their original sentence order.
     *
     * Names are never eligible: a proper noun is almost never in the Oxford lists, so it would
     * otherwise dominate [rankOf]'s "unknown = hardest" tier and get picked constantly even though
     * translating a name isn't useful for a learner. A word is treated as a name and dropped from
     * consideration entirely when it's capitalized *and* unrecognized - sentence position isn't a
     * reliable enough signal on its own, since transcripts are chunked into segments at pause
     * points rather than true sentence boundaries, so a name can end up "sentence-initial" purely
     * by where the transcription happened to split. A capitalized *known* word (e.g. "The" opening
     * a sentence) stays eligible either way, since being in the vocabulary at all means ordinary
     * capitalization - not name-ness - is what's going on.
     */
    fun orderHardestFirst(
        words: List<WordTiming>,
        rankOf: (String) -> Int,
        isKnownWord: (String) -> Boolean,
    ): List<WordTiming> =
        words
            .filter { it.word.any(Char::isLetter) }
            .withIndex()
            .filterNot { (_, word) -> looksLikeProperNoun(word.word) && !isKnownWord(word.word) }
            .sortedWith(
                compareByDescending<IndexedValue<WordTiming>> { rankOf(it.value.word) }
                    .thenBy { it.index },
            )
            .map { it.value }

    private fun looksLikeProperNoun(word: String): Boolean =
        word.firstOrNull(Char::isLetter)?.isUpperCase() == true
}
