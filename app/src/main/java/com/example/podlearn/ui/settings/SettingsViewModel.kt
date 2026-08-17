package com.example.podlearn.ui.settings

import androidx.lifecycle.ViewModel
import com.example.podlearn.data.repository.SettingsRepository
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
}
