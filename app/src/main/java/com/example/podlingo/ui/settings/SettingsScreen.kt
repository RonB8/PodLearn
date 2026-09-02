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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.data.repository.ThemeMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenUnknownWords: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val hardWordModeEnabled by viewModel.hardWordModeEnabled.collectAsStateWithLifecycle()
    val autoFullSentenceEnabled by viewModel.autoFullSentenceEnabled.collectAsStateWithLifecycle()
    val autoPlayNextEnabled by viewModel.autoPlayNextEnabled.collectAsStateWithLifecycle()
    val autoTranslateReadAloudEnabled by viewModel.autoTranslateReadAloudEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val unknownWordCount by viewModel.unknownWordCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxWidth().padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = { Text("Theme") },
                supportingContent = {
                    ThemeModeSelector(
                        selected = themeMode,
                        onSelected = viewModel::setThemeMode,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                },
            )
            SettingsToggleItem(
                title = "Translate hardest word only",
                description = "On trigger, translate just the hardest word in the sentence " +
                    "(by Oxford CEFR level). Trigger again right away on the same " +
                    "sentence to reveal the next-hardest word.",
                checked = hardWordModeEnabled,
                onCheckedChange = viewModel::setHardWordModeEnabled,
            )
            SettingsToggleItem(
                title = "Auto full-sentence for hard sentences",
                description = "Within hard-word mode: if a sentence has ${AppDefaults.AUTO_FULL_SENTENCE_HARD_WORD_COUNT} " +
                    "or more hard words, translate the whole sentence instead of one word at a time.",
                checked = autoFullSentenceEnabled,
                onCheckedChange = viewModel::setAutoFullSentenceEnabled,
                switchEnabled = hardWordModeEnabled,
            )
            SettingsToggleItem(
                title = "Auto-play next episode",
                description = "When an episode finishes, automatically start the next one in the podcast.",
                checked = autoPlayNextEnabled,
                onCheckedChange = viewModel::setAutoPlayNextEnabled,
            )
            SettingsToggleItem(
                title = "Read auto-translated words aloud",
                description = "When Auto translate finds a word you don't know, pause and read it aloud " +
                    "like the manual trigger does - just the word if hard-word mode is on, " +
                    "or the whole sentence otherwise.",
                checked = autoTranslateReadAloudEnabled,
                onCheckedChange = viewModel::setAutoTranslateReadAloudEnabled,
            )
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenUnknownWords),
                headlineContent = { Text("Words you don't know") },
                supportingContent = { Text("View and edit the words flagged for auto-translation.") },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(unknownWordCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                },
            )
        }
    }
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
                        Icon(Icons.Outlined.Info, contentDescription = "About $title")
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
    modifier: Modifier = Modifier,
) {
    val options = listOf(ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark", ThemeMode.SYSTEM to "System")
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) },
            )
        }
    }
}
