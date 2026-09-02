package com.example.podlingo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.data.repository.AppLanguage
import com.example.podlingo.data.repository.SettingsRepository
import com.example.podlingo.data.repository.ThemeMode
import com.example.podlingo.ui.navigation.PodLingoNavHost
import com.example.podlingo.ui.strings.EnglishStrings
import com.example.podlingo.ui.strings.HebrewStrings
import com.example.podlingo.ui.strings.LocalAppStrings
import com.example.podlingo.ui.theme.PodLingoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // enableEdgeToEdge() only picks status/nav bar icon contrast from the system theme at
            // launch - it never tracks the app's own Light/Dark override, so choosing Light while
            // the system is in Dark (or vice versa) left the icons using the wrong, invisible
            // contrast. Re-applying this on every theme change keeps it in sync.
            SideEffect {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
            val appLanguage by settingsRepository.appLanguage.collectAsStateWithLifecycle()
            val strings = if (appLanguage == AppLanguage.HEBREW) HebrewStrings else EnglishStrings
            // The whole app mirrors to RTL for Hebrew except the player's timeline/controls and the
            // mini-player, which pin themselves back to Ltr locally (see PlaybackControls, MiniPlayerBar).
            val layoutDirection = if (appLanguage == AppLanguage.HEBREW) LayoutDirection.Rtl else LayoutDirection.Ltr
            PodLingoTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalAppStrings provides strings,
                    LocalLayoutDirection provides layoutDirection,
                ) {
                    PodLingoNavHost()
                }
            }
        }
    }

    /** Without this, the playback notification (and lock-screen controls tied to it) can't show on Android 13+. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
