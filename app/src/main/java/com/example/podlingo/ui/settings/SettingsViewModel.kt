package com.example.podlingo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.repository.SettingsRepository
import com.example.podlingo.data.repository.ThemeMode
import com.example.podlingo.data.repository.WordKnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    wordKnowledgeRepository: WordKnowledgeRepository,
) : ViewModel() {

    val unknownWordCount: StateFlow<Int> = wordKnowledgeRepository.observeUnknownWords()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hardWordModeEnabled: StateFlow<Boolean> = settingsRepository.hardWordModeEnabled

    fun setHardWordModeEnabled(enabled: Boolean) {
        settingsRepository.setHardWordModeEnabled(enabled)
    }

    val autoFullSentenceEnabled: StateFlow<Boolean> = settingsRepository.autoFullSentenceEnabled

    fun setAutoFullSentenceEnabled(enabled: Boolean) {
        settingsRepository.setAutoFullSentenceEnabled(enabled)
    }

    val autoPlayNextEnabled: StateFlow<Boolean> = settingsRepository.autoPlayNextEnabled

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        settingsRepository.setAutoPlayNextEnabled(enabled)
    }

    val autoTranslateReadAloudEnabled: StateFlow<Boolean> = settingsRepository.autoTranslateReadAloudEnabled

    fun setAutoTranslateReadAloudEnabled(enabled: Boolean) {
        settingsRepository.setAutoTranslateReadAloudEnabled(enabled)
    }

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
