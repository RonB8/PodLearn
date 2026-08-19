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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val hardWordModeEnabled by viewModel.hardWordModeEnabled.collectAsStateWithLifecycle()
    val autoPlayNextEnabled by viewModel.autoPlayNextEnabled.collectAsStateWithLifecycle()

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
