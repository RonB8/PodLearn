package com.example.podlingo.ui.vocabulary

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
class KnownWordsViewModel @Inject constructor(
    private val wordKnowledgeRepository: WordKnowledgeRepository,
) : VocabWordListViewModel(wordKnowledgeRepository) {

    val knownWords: StateFlow<List<WordKnowledgeEntity>> =
        wordKnowledgeRepository.observeKnownWords()
            .onEach { translationFetcher.fetchMissing(it) }
            .sortedForVocabList(sortModeFlow)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Removing a word from this list means "I don't know it anymore" - it starts auto-translating again, same as any freshly-flagged word. */
    fun markUnknown(word: String) {
        viewModelScope.launch { wordKnowledgeRepository.markUnknown(word) }
    }

    /** Manually flags a word without waiting for it to come up in a quiz/calibration. */
    fun addWord(word: String) {
        viewModelScope.launch { wordKnowledgeRepository.markKnown(word) }
    }
}
