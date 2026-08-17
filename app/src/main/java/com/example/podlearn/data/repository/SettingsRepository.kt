package com.example.podlearn.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide user preferences. Backed by SharedPreferences rather than DataStore - a couple of
 * plain flags don't warrant the extra dependency.
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hardWordModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_HARD_WORD_MODE, false))
    val hardWordModeEnabled: StateFlow<Boolean> = _hardWordModeEnabled.asStateFlow()

    fun setHardWordModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HARD_WORD_MODE, enabled).apply()
        _hardWordModeEnabled.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "podlearn_settings"
        private const val KEY_HARD_WORD_MODE = "hard_word_mode_enabled"
    }
}
