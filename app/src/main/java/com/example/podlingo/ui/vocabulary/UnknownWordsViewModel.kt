package com.example.podlingo.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.entity.WordKnowledgeEntity
import com.example.podlingo.data.repository.WordKnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UnknownWordsViewModel @Inject constructor(
    private val wordKnowledgeRepository: WordKnowledgeRepository,
) : ViewModel() {

    // A word only ever gets fetched lazily during playback/quiz when it's actually needed - a
    // word marked unknown via calibration but never re-encountered would otherwise sit here
    // forever with no translation to show. Fetching once per word here (guarded so a failed fetch
    // isn't retried every time this Flow re-emits for some other word's change) still costs at
    // most one OpenAI call per word, ever, same as everywhere else - viewing this screen is a real
    // need for the translation, not a speculative pre-fetch.
    private val fetchAttempted = mutableSetOf<String>()

    val unknownWords: StateFlow<List<WordKnowledgeEntity>> = wordKnowledgeRepository.observeUnknownWords()
        .onEach { words ->
            words.filter { it.hebrewTranslation == null && it.word !in fetchAttempted }
                .forEach { entry ->
                    fetchAttempted += entry.word
                    viewModelScope.launch { wordKnowledgeRepository.getOrFetchTranslation(entry.word) }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Removing a word from this list means "I know it now" - it stops auto-translating and (like any known word) is never asked about again in calibration. */
    fun markKnown(word: String) {
        viewModelScope.launch { wordKnowledgeRepository.markKnown(word) }
    }
}
