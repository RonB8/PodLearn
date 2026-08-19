package com.example.podlingo.data.repository

import android.content.Context
import com.example.podlingo.core.ElementaryFunctionWords
import com.example.podlingo.core.EnglishStemmer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Looks up each word's CEFR difficulty level from the bundled Oxford 3000/5000 word lists
 * (assets/oxford_word_levels.txt, generated from the source PDFs in Oxford/). Words absent from
 * those lists are treated as harder than any listed word - not being common enough to make the
 * Oxford lists is itself a difficulty signal.
 *
 * The lists only hold dictionary headwords ("member", "vote"), but a transcript speaks inflected
 * surface forms ("members", "voted"). A literal-only lookup would misclassify those as unranked
 * (hardest) even though their base form is elementary, so a miss falls through [EnglishStemmer]'s
 * base-form candidates before giving up.
 *
 * That still isn't enough for closed-class words: the bundled list omits "the" outright, and
 * omits every irregular form of "be"/"do"/"have" ("is", "was", "been", "does", "had", ...) - no
 * suffix stemmer can derive those from their headword either, since they're irregular rather than
 * inflected. Left unhandled, the single most common words in English would rank as the hardest
 * ones in a sentence. [ElementaryFunctionWords] short-circuits that closed, finite set to the
 * easiest rank before either lookup runs.
 */
@Singleton
class WordDifficultyRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val levelRankByWord: Map<String, Int> by lazy { loadLevels() }

    /** Lower is easier; [UNKNOWN_RANK] for words not in the Oxford lists (the hardest tier). */
    fun rankOf(word: String): Int {
        val normalized = normalize(word)
        if (normalized.isEmpty()) return UNKNOWN_RANK
        if (ElementaryFunctionWords.contains(normalized)) return EASIEST_RANK
        levelRankByWord[normalized]?.let { return it }
        for (candidate in EnglishStemmer.candidateBaseForms(normalized)) {
            levelRankByWord[candidate]?.let { return it }
        }
        return UNKNOWN_RANK
    }

    private fun loadLevels(): Map<String, Int> {
        val map = HashMap<String, Int>(6000)
        context.assets.open(ASSET_FILE_NAME).bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val tab = line.indexOf('\t')
                if (tab <= 0) return@forEach
                val word = line.substring(0, tab)
                val rank = CEFR_RANK[line.substring(tab + 1)] ?: return@forEach
                map[word] = rank
            }
        }
        return map
    }

    private fun normalize(word: String): String =
        word.lowercase().filter { it.isLetter() || it == '\'' || it == '-' }

    companion object {
        const val UNKNOWN_RANK = 5
        private const val EASIEST_RANK = 0
        private const val ASSET_FILE_NAME = "oxford_word_levels.txt"
        private val CEFR_RANK = mapOf("A1" to 0, "A2" to 1, "B1" to 2, "B2" to 3, "C1" to 4)
    }
}
