package com.example.podlingo.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.entity.WordKnowledgeEntity
import com.example.podlingo.data.repository.WordKnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UnknownWordsViewModel @Inject constructor(
    private val wordKnowledgeRepository: WordKnowledgeRepository,
) : ViewModel() {

    val unknownWords: StateFlow<List<WordKnowledgeEntity>> = wordKnowledgeRepository.observeUnknownWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Removing a word from this list means "I know it now" - it stops auto-translating and (like any known word) is never asked about again in calibration. */
    fun markKnown(word: String) {
        viewModelScope.launch { wordKnowledgeRepository.markKnown(word) }
    }
}
