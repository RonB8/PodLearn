package com.example.podlingo.ui.player

/**
 * One step of the "which of this episode's hardest words do you already know" flow - see
 * [PlayerViewModel.toggleAutoTranslate]. [tierRank] is the Oxford CEFR rank being shown (5 =
 * unranked/hardest .. 0 = A1), [words] are that tier's words still undecided, and [selected] are
 * the ones the user has tapped as "I don't know this" so far.
 */
data class VocabCalibrationState(
    val tierRank: Int,
    val words: List<String>,
    val selected: Set<String> = emptySet(),
)

/** A transient, non-blocking translation shown while an unknown word plays - see [PlayerViewModel]'s position-polling popup logic. */
data class WordTranslationPopup(
    val word: String,
    val translation: String,
)

data class VocabQuizQuestion(
    val word: String,
    val correctAnswer: String,
    /** All 4 options, correct answer included, already shuffled. */
    val options: List<String>,
)

data class VocabQuizState(
    val questions: List<VocabQuizQuestion>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    /** The option the user tapped for the current question, or null if not answered yet - drives the right/wrong reveal before "Next". */
    val answeredThisQuestion: String? = null,
    val finished: Boolean = false,
    /** True only for the pre-episode start quiz - hides the "Next" button (the ViewModel advances on a short delay instead) and enables the tier-cascade fields below. */
    val autoAdvance: Boolean = false,
    /** Start quiz only: easier tiers not yet asked about, hardest-of-what's-left first - each is a bunch of words, pulled in only if [currentTierWrongCount]/[currentTierTotal] for the tier just finished exceeds the cascade threshold. */
    val remainingTiers: List<List<String>> = emptyList(),
    /** Start quiz only: wrong-answer tally for the tier currently in progress (resets to 0 whenever a new tier's questions are appended). */
    val currentTierWrongCount: Int = 0,
    val currentTierTotal: Int = 0,
)
