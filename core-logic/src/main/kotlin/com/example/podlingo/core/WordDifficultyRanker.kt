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
     * found" means hardest). Words with no letters (stray punctuation tokens) are dropped. Ties
     * keep their original sentence order.
     */
    fun orderHardestFirst(words: List<WordTiming>, rankOf: (String) -> Int): List<WordTiming> =
        words
            .filter { it.word.any(Char::isLetter) }
            .withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<WordTiming>> { rankOf(it.value.word) }
                    .thenBy { it.index },
            )
            .map { it.value }
}
