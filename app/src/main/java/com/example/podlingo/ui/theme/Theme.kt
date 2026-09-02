package com.example.podlingo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun PodLingoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color (Material You) pulls its palette from the device wallpaper, which on this
    // device skews purple - overriding just secondaryContainer below couldn't fix that everywhere
    // (primary, buttons, progress indicators, etc. would still be whatever the wallpaper produced).
    // Off by default so the app always renders PodLingo's own blue palette.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    // secondaryContainer/onSecondaryContainer drive every "selected" indicator in the app (the
    // Home/Search/Library tab bar, FilterChip selection in the vocab calibration panel, etc.) - the
    // algorithmically-generated tone for these roles is too pale against a grey background to read
    // as "selected" at a glance. Pinned to a solid, high-contrast pair here so the selected state is
    // always clearly legible regardless of theme.
    val colorScheme = baseColorScheme.copy(
        secondaryContainer = if (darkTheme) Blue80 else Blue40,
        onSecondaryContainer = if (darkTheme) Blue40 else androidx.compose.ui.graphics.Color.White,
    )

    // PodLingo's content (podcast titles, media transport controls) is inherently LTR-oriented
    // regardless of the device's system locale - a seek-back/seek-forward pair mirroring on an
    // RTL locale is wrong (that's a spatial/temporal timeline metaphor, not a text-direction one),
    // and it was swapping the two skip buttons' screen positions. Hebrew translation text inside
    // still renders correctly via Unicode bidi at the glyph level; this only affects layout order.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}