package com.example.podlingo.core

/**
 * Closed-class function words - articles, pronouns, the irregular forms of "be"/"do"/"have",
 * common prepositions/conjunctions/modals - that are unambiguously elementary but show up
 * inconsistently in CEFR vocabulary lists. Those lists are built for content vocabulary, not
 * grammar: "the" being present says nothing about whether "is", "was", or "been" also made the
 * cut (in the bundled Oxford list, none of them did, since they're irregular inflections of "be"
 * that no suffix-stripping stemmer could derive from it either). Treating an omission from that
 * data as "hardest, unranked" - [WordDifficultyRepository]'s fallback for content words - is
 * wrong here: this is a closed, finite set with no real ambiguity about how hard "is" is.
 */
object ElementaryFunctionWords {

    fun contains(normalizedWord: String): Boolean = WORDS.contains(normalizedWord)

    private val WORDS = setOf(
        // Articles
        "a", "an", "the",
        // Personal / possessive / demonstrative / relative pronouns
        "i", "you", "he", "she", "it", "we", "they",
        "me", "him", "her", "us", "them",
        "my", "your", "his", "its", "our", "their", "mine", "yours", "hers", "ours", "theirs",
        "this", "that", "these", "those", "who", "whom", "whose", "which", "what",
        // "be" - irregular
        "am", "is", "are", "was", "were", "been", "being",
        // "have" - irregular
        "has", "had", "having",
        // "do" - irregular
        "does", "did", "doing", "done",
        // Common prepositions
        "to", "of", "in", "on", "at", "by", "for", "with", "about", "against", "between",
        "into", "through", "during", "before", "after", "above", "below", "from", "up", "down",
        "over", "under",
        // Common conjunctions
        "and", "but", "or", "nor", "so", "because", "if", "though", "although", "while",
        // Common modals
        "can", "could", "will", "would", "shall", "should", "may", "might", "must",
    )
}
