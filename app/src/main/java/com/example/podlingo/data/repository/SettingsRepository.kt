package com.example.podlingo.data.repository

import android.content.Context
import com.example.podlingo.config.AppDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class AppLanguage { ENGLISH, HEBREW }

/** "he" is the modern ISO code for Hebrew; "iw" is the older code some Android versions still report. */
private fun systemDefaultAppLanguage(): AppLanguage =
    if (Locale.getDefault().language in setOf("he", "iw")) AppLanguage.HEBREW else AppLanguage.ENGLISH

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

    private val _showSentenceTranslationsEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_SHOW_SENTENCE_TRANSLATIONS, false))
    val showSentenceTranslationsEnabled: StateFlow<Boolean> = _showSentenceTranslationsEnabled.asStateFlow()

    fun setShowSentenceTranslationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_SENTENCE_TRANSLATIONS, enabled).apply()
        _showSentenceTranslationsEnabled.value = enabled
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

    /** Defaults to the device's system locale on first read (Hebrew if the phone is set to Hebrew), then stays put until the user overrides it here. */
    private val _appLanguage = MutableStateFlow(
        prefs.getString(KEY_APP_LANGUAGE, null)?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: systemDefaultAppLanguage(),
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.name).apply()
        _appLanguage.value = language
    }

    /** Downloaded-episode storage cap - see [com.example.podlingo.data.repository.EpisodeStorageManager]. */
    private val _storageLimitBytes = MutableStateFlow(
        prefs.getLong(KEY_STORAGE_LIMIT_BYTES, AppDefaults.DEFAULT_STORAGE_LIMIT_BYTES),
    )
    val storageLimitBytes: StateFlow<Long> = _storageLimitBytes.asStateFlow()

    fun setStorageLimitBytes(bytes: Long) {
        val clamped = bytes.coerceIn(AppDefaults.MIN_STORAGE_LIMIT_BYTES, AppDefaults.MAX_STORAGE_LIMIT_BYTES)
        prefs.edit().putLong(KEY_STORAGE_LIMIT_BYTES, clamped).apply()
        _storageLimitBytes.value = clamped
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
        private const val KEY_SHOW_SENTENCE_TRANSLATIONS = "show_sentence_translations_enabled"
        private const val KEY_AUTO_PLAY_NEXT = "auto_play_next_enabled"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_STORAGE_LIMIT_BYTES = "storage_limit_bytes"
        private const val KEY_LAST_ROUTE = "last_route"
        private const val KEY_LAST_TAB_INDEX = "last_tab_index"
    }
}
