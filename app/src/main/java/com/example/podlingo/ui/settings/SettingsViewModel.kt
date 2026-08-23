package com.example.podlingo.ui.settings

import androidx.lifecycle.ViewModel
import com.example.podlingo.data.repository.SettingsRepository
import com.example.podlingo.data.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val hardWordModeEnabled: StateFlow<Boolean> = settingsRepository.hardWordModeEnabled

    fun setHardWordModeEnabled(enabled: Boolean) {
        settingsRepository.setHardWordModeEnabled(enabled)
    }

    val autoPlayNextEnabled: StateFlow<Boolean> = settingsRepository.autoPlayNextEnabled

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        settingsRepository.setAutoPlayNextEnabled(enabled)
    }

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }
}
