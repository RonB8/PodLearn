package com.example.podlingo.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.ui.strings.LocalAppStrings

@Composable
fun AddToPlaylistDialog(
    episodeId: String,
    onDismiss: () -> Unit,
    viewModel: AddToPlaylistViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val membershipFlow = remember(episodeId) { viewModel.membershipFlow(episodeId) }
    val membership by membershipFlow.collectAsStateWithLifecycle(initialValue = emptySet())
    var newPlaylistName by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addToPlaylistTitle) },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        text = strings.noPlaylistsYetCreateBelow,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            val inPlaylist = playlist.id in membership
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleMembership(playlist.id, episodeId, inPlaylist) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = inPlaylist,
                                    onCheckedChange = { viewModel.toggleMembership(playlist.id, episodeId, inPlaylist) },
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = playlist.name, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text(strings.newPlaylistFieldLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            viewModel.createPlaylistAndAdd(newPlaylistName, episodeId)
                            newPlaylistName = ""
                        },
                        enabled = newPlaylistName.isNotBlank(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = strings.createAndAddContentDescription)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.done) }
        },
    )
}
