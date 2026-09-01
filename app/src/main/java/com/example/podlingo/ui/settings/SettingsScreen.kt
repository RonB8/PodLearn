@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.data.repository.ThemeMode

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val hardWordModeEnabled by viewModel.hardWordModeEnabled.collectAsStateWithLifecycle()
    val autoFullSentenceEnabled by viewModel.autoFullSentenceEnabled.collectAsStateWithLifecycle()
    val autoPlayNextEnabled by viewModel.autoPlayNextEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

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
        Column(modifier = Modifier.fillMaxWidth().padding(padding)) {
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
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = { Text("Translate hardest word only") },
                supportingContent = {
                    Text(
                        "On trigger, translate just the hardest word in the sentence " +
                            "(by Oxford CEFR level). Trigger again right away on the same " +
                            "sentence to reveal the next-hardest word.",
                    )
                },
                trailingContent = {
                    Switch(
                        checked = hardWordModeEnabled,
                        onCheckedChange = viewModel::setHardWordModeEnabled,
                    )
                },
            )
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = { Text("Auto full-sentence for hard sentences") },
                supportingContent = {
                    Text(
                        "Within hard-word mode: if a sentence has ${AppDefaults.AUTO_FULL_SENTENCE_HARD_WORD_COUNT} " +
                            "or more hard words, translate the whole sentence instead of one word at a time.",
                    )
                },
                trailingContent = {
                    Switch(
                        checked = autoFullSentenceEnabled,
                        onCheckedChange = viewModel::setAutoFullSentenceEnabled,
                        enabled = hardWordModeEnabled,
                    )
                },
            )
            ListItem(
                modifier = Modifier.fillMaxWidth(),
                headlineContent = { Text("Auto-play next episode") },
                supportingContent = {
                    Text("When an episode finishes, automatically start the next one in the podcast.")
                },
                trailingContent = {
                    Switch(
                        checked = autoPlayNextEnabled,
                        onCheckedChange = viewModel::setAutoPlayNextEnabled,
                    )
                },
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
