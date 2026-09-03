@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.data.repository.AppLanguage
import com.example.podlingo.data.repository.ThemeMode
import com.example.podlingo.ui.strings.AppStrings
import com.example.podlingo.ui.strings.LocalAppStrings

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUnknownWords: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val hardWordModeEnabled by viewModel.hardWordModeEnabled.collectAsStateWithLifecycle()
    val autoFullSentenceEnabled by viewModel.autoFullSentenceEnabled.collectAsStateWithLifecycle()
    val autoPlayNextEnabled by viewModel.autoPlayNextEnabled.collectAsStateWithLifecycle()
    val autoTranslateReadAloudEnabled by viewModel.autoTranslateReadAloudEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val unknownWordCount by viewModel.unknownWordCount.collectAsStateWithLifecycle()
    val storageLimitBytes by viewModel.storageLimitBytes.collectAsStateWithLifecycle()
    val storageUsedBytes by viewModel.storageUsedBytes.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = { Text(strings.languageLabel) },
                supportingContent = {
                    LanguageSelector(
                        selected = appLanguage,
                        onSelected = viewModel::setAppLanguage,
                        strings = strings,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                },
            )
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = { Text(strings.themeLabel) },
                supportingContent = {
                    ThemeModeSelector(
                        selected = themeMode,
                        onSelected = viewModel::setThemeMode,
                        strings = strings,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                },
            )
            SettingsToggleItem(
                title = strings.hardWordModeTitle,
                description = strings.hardWordModeDescription,
                checked = hardWordModeEnabled,
                onCheckedChange = viewModel::setHardWordModeEnabled,
                strings = strings,
            )
            SettingsToggleItem(
                title = strings.autoFullSentenceTitle,
                description = strings.autoFullSentenceDescription(AppDefaults.AUTO_FULL_SENTENCE_HARD_WORD_COUNT),
                checked = autoFullSentenceEnabled,
                onCheckedChange = viewModel::setAutoFullSentenceEnabled,
                switchEnabled = hardWordModeEnabled,
                strings = strings,
            )
            SettingsToggleItem(
                title = strings.autoPlayNextTitle,
                description = strings.autoPlayNextDescription,
                checked = autoPlayNextEnabled,
                onCheckedChange = viewModel::setAutoPlayNextEnabled,
                strings = strings,
            )
            SettingsToggleItem(
                title = strings.autoTranslateReadAloudTitle,
                description = strings.autoTranslateReadAloudDescription,
                checked = autoTranslateReadAloudEnabled,
                onCheckedChange = viewModel::setAutoTranslateReadAloudEnabled,
                strings = strings,
            )
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenUnknownWords),
                headlineContent = { Text(strings.unknownWordsTitle) },
                supportingContent = { Text(strings.unknownWordsDescription) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(unknownWordCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                },
            )
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = { Text(strings.storageTitle) },
                supportingContent = {
                    StorageLimitSection(
                        usedBytes = storageUsedBytes,
                        limitBytes = storageLimitBytes,
                        onLimitChanged = viewModel::setStorageLimitBytes,
                        strings = strings,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                },
            )
        }
    }
}

/** Shows current usage against the configured cap and lets the user drag that cap between [AppDefaults.MIN_STORAGE_LIMIT_BYTES] and [AppDefaults.MAX_STORAGE_LIMIT_BYTES]. */
@Composable
private fun StorageLimitSection(
    usedBytes: Long,
    limitBytes: Long,
    onLimitChanged: (Long) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
    // Local drag state so the label tracks the thumb smoothly - only committed to the ViewModel
    // (and persisted) once the drag ends, not on every intermediate value.
    var pendingLimitBytes by remember(limitBytes) { mutableStateOf(limitBytes) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = strings.storageUsageLabel(formatBytes(usedBytes), formatBytes(pendingLimitBytes)),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = pendingLimitBytes.toFloat(),
            onValueChange = { pendingLimitBytes = it.toLong() },
            onValueChangeFinished = { onLimitChanged(pendingLimitBytes) },
            valueRange = AppDefaults.MIN_STORAGE_LIMIT_BYTES.toFloat()..AppDefaults.MAX_STORAGE_LIMIT_BYTES.toFloat(),
            steps = STORAGE_LIMIT_SLIDER_STEPS,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = strings.storageLimitDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 500 MB increments between the min and max storage limit, excluding both endpoints. */
private const val STORAGE_LIMIT_SLIDER_STEPS = 18

private fun formatBytes(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 1) "%.1f GB".format(gb) else "%.0f MB".format(bytes / (1024.0 * 1024.0))
}

/**
 * A toggle setting whose explanatory [description] stays collapsed until the info icon is tapped,
 * so the settings list reads as a short list of titles rather than a wall of paragraphs.
 */
@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    strings: AppStrings,
    switchEnabled: Boolean = true,
) {
    var infoExpanded by remember { mutableStateOf(false) }
    Column {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = { Text(title) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { infoExpanded = !infoExpanded }) {
                        Icon(Icons.Outlined.Info, contentDescription = strings.aboutContentDescription(title))
                    }
                    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = switchEnabled)
                }
            },
        )
        AnimatedVisibility(visible = infoExpanded) {
            Text(
                text = description,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        ThemeMode.LIGHT to strings.themeLight,
        ThemeMode.DARK to strings.themeDark,
        ThemeMode.SYSTEM to strings.themeSystem,
    )
    // Kept Ltr regardless of app language - mirroring this row to RTL just swapped which end each
    // segment's rounded corner sat on without changing anything meaningful, reading as broken.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick = { onSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                    icon = {},
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        AppLanguage.ENGLISH to strings.languageEnglish,
        AppLanguage.HEBREW to strings.languageHebrew,
    )
    // Kept Ltr regardless of app language, same reasoning as ThemeModeSelector above.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (language, label) ->
                SegmentedButton(
                    selected = selected == language,
                    onClick = { onSelected(language) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(label) },
                    icon = {},
                )
            }
        }
    }
}
