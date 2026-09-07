package com.example.podlingo.ui.player

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
)

enum class WordCheckTab { QUIZ, SIMPLE }

/**
 * One tier of the merged pre-episode "Word Check" assessment - see [PlayerViewModel.beginWordCheck].
 * [words] is this tier's full word pool (brand-new/undecided words and previously-flagged-unknown
 * ones, combined), hardest-tier-first. [questions] is however many of those words got a buildable
 * multiple-choice question (see [VocabQuizBuilder] - a word is dropped if too few real distractors
 * exist yet). The Quiz and Simple tabs both work through the very same [words] list - [quizCorrectWords],
 * [tapSelected] and [skipped] are shared between them so switching tabs mid-tier never loses
 * progress, and a word answered correctly in one tab shows resolved in the other.
 */
data class WordCheckState(
    val tab: WordCheckTab,
    val words: List<String>,
    val questions: List<VocabQuizQuestion>,
    val currentIndex: Int = 0,
    val answeredThisQuestion: String? = null,
    /** Answered correctly via the Quiz tab this tier - already marked known; renders read-only/green-checked in the Simple tab. */
    val quizCorrectWords: Set<String> = emptySet(),
    /** The Simple tab's "I don't know this" selection - seeded with words answered wrong in the Quiz tab, freely editable in either direction from there. */
    val tapSelected: Set<String> = emptySet(),
    /** Explicitly skipped in the Quiz tab - excluded from [PlayerViewModel.onWordCheckContinue]'s default-to-known resolution, so a skipped word resurfaces on the next fresh Word Check instead of silently ending up "known". Tapping the word in the Simple tab clears this. */
    val skipped: Set<String> = emptySet(),
    /** Easier tiers not yet asked about, hardest-of-what's-left first - pulled in by [PlayerViewModel.onWordCheckContinue] once the tier just finished. */
    val remainingTiers: List<List<String>> = emptyList(),
)
