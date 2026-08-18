package com.example.podlingo.core

/**
 * Lightweight heuristic English stemmer. The Oxford CEFR lists ([WordDifficultyRanker]'s data
 * source) only contain dictionary headwords - singular nouns, base-form verbs - but a transcript
 * speaks inflected surface forms: "members" not "member", "voted" not "vote". A literal lookup
 * misses those and would wrongly treat them as unranked (hardest), even though their base form is
 * elementary. This generates the base-form candidates worth trying before giving up, in order of
 * which rule is most likely correct for that suffix. It's not a full Porter stemmer - just enough
 * to catch -s/-es/-ies, -ing, -ed, -er/-est, and possessive 's.
 */
object EnglishStemmer {

    fun candidateBaseForms(word: String): List<String> {
        if (word.length < 3) return emptyList()
        val candidates = LinkedHashSet<String>()

        if (word.endsWith("'s") && word.length > 2) candidates += word.dropLast(2)

        when {
            word.endsWith("ies") && word.length > 4 -> candidates += word.dropLast(3) + "y"
            word.endsWith("es") && word.length > 3 -> {
                candidates += word.dropLast(2)
                candidates += word.dropLast(1)
            }
            word.endsWith("s") && !word.endsWith("ss") && word.length > 2 -> candidates += word.dropLast(1)
        }

        if (word.endsWith("ing") && word.length > 5) {
            val stem = word.dropLast(3)
            candidates += stem
            candidates += stem + "e"
            addDeduplicatedConsonant(candidates, stem)
        }

        if (word.endsWith("ied") && word.length > 4) {
            candidates += word.dropLast(3) + "y"
        } else if (word.endsWith("ed") && word.length > 4) {
            val stem = word.dropLast(2)
            candidates += stem
            candidates += stem + "e"
            addDeduplicatedConsonant(candidates, stem)
        }

        if (word.endsWith("est") && word.length > 4) {
            val stem = word.dropLast(3)
            candidates += stem
            candidates += stem + "e"
        } else if (word.endsWith("er") && word.length > 3) {
            val stem = word.dropLast(2)
            candidates += stem
            candidates += stem + "e"
        }

        return candidates.toList()
    }

    /** "running" / "stopped" style doubled final consonant: stem "runn"/"stopp" -> "run"/"stop". */
    private fun addDeduplicatedConsonant(candidates: MutableSet<String>, stem: String) {
        if (stem.length > 1 && stem.last() == stem[stem.length - 2] && stem.last() !in VOWELS) {
            candidates += stem.dropLast(1)
        }
    }

    private val VOWELS = charArrayOf('a', 'e', 'i', 'o', 'u')
}
