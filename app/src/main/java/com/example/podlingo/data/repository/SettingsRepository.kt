package com.example.podlingo.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

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

    private val _autoFullSentenceEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_AUTO_FULL_SENTENCE, true))
    val autoFullSentenceEnabled: StateFlow<Boolean> = _autoFullSentenceEnabled.asStateFlow()

    fun setAutoFullSentenceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_FULL_SENTENCE, enabled).apply()
        _autoFullSentenceEnabled.value = enabled
    }

    private val _autoTranslateEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_TRANSLATE, false))
    val autoTranslateEnabled: StateFlow<Boolean> = _autoTranslateEnabled.asStateFlow()

    fun setAutoTranslateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_TRANSLATE, enabled).apply()
        _autoTranslateEnabled.value = enabled
    }

    private val _autoTranslateReadAloudEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_AUTO_TRANSLATE_READ_ALOUD, false))
    val autoTranslateReadAloudEnabled: StateFlow<Boolean> = _autoTranslateReadAloudEnabled.asStateFlow()

    fun setAutoTranslateReadAloudEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_TRANSLATE_READ_ALOUD, enabled).apply()
        _autoTranslateReadAloudEnabled.value = enabled
    }

    private val _autoPlayNextEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true))
    val autoPlayNextEnabled: StateFlow<Boolean> = _autoPlayNextEnabled.asStateFlow()

    fun setAutoPlayNextEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, enabled).apply()
        _autoPlayNextEnabled.value = enabled
    }

    private val _themeMode = MutableStateFlow(
        prefs.getString(KEY_THEME_MODE, null)?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    /**
     * The last argument-free top-level screen the user had open, so a cold start (the process was
     * killed in the background - common on Samsung's aggressive battery management, or the user
     * swiped the app away in Recents) can return there instead of always resetting to the podcast
     * list. Android's own savedInstanceState restoration only covers the "process still tracked
     * as resumable" case, not either of those.
     */
    var lastRoute: String?
        get() = prefs.getString(KEY_LAST_ROUTE, null)
        set(value) {
            prefs.edit().putString(KEY_LAST_ROUTE, value).apply()
        }

    /** Which of the Home/Search/Library tabs was showing, restored on a cold start alongside [lastRoute]. */
    var lastTabIndex: Int
        get() = prefs.getInt(KEY_LAST_TAB_INDEX, 0)
        set(value) {
            prefs.edit().putInt(KEY_LAST_TAB_INDEX, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "podlingo_settings"
        private const val KEY_HARD_WORD_MODE = "hard_word_mode_enabled"
        private const val KEY_AUTO_FULL_SENTENCE = "auto_full_sentence_enabled"
        private const val KEY_AUTO_TRANSLATE = "auto_translate_enabled"
        private const val KEY_AUTO_TRANSLATE_READ_ALOUD = "auto_translate_read_aloud_enabled"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_ROUTE = "last_route"
        private const val KEY_LAST_TAB_INDEX = "last_tab_index"
    }
}
