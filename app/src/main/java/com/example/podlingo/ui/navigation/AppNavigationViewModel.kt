package com.example.podlingo.ui.navigation

import androidx.lifecycle.ViewModel
import com.example.podlingo.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Activity-scoped (created directly inside [PodLingoNavHost], not any single route) so it can
 * read the last-visited screen once at cold start and keep persisting it as navigation happens,
 * independent of which destination is currently composed.
 */
@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val lastRoute: String? = settingsRepository.lastRoute

    fun rememberRoute(route: String) {
        settingsRepository.lastRoute = route
    }
}
