package com.example.podlingo.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.example.podlingo.data.local.dao.PlaylistWithCount
import com.example.podlingo.ui.strings.AppStrings
import com.example.podlingo.ui.strings.LocalAppStrings

/** The "Playlists" section within the Library tab. */
@Composable
fun PlaylistsContent(
    onOpenPlaylist: (String) -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }

    if (showCreateDialog) {
        NamePlaylistDialog(
            title = strings.newPlaylistTitle,
            initialName = "",
            onConfirm = { name -> viewModel.createPlaylist(name) },
            onDismiss = { showCreateDialog = false },
            strings = strings,
        )
    }
    renamingPlaylist?.let { playlist ->
        NamePlaylistDialog(
            title = strings.renamePlaylistTitle,
            initialName = playlist.name,
            onConfirm = { name -> viewModel.renamePlaylist(playlist.id, name) },
            onDismiss = { renamingPlaylist = null },
            strings = strings,
        )
    }
    deletingPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            title = { Text(strings.deletePlaylistConfirmTitle(playlist.name)) },
            text = { Text(strings.deletePlaylistConfirmText) },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlaylist(playlist.id); deletingPlaylist = null }) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlaylist = null }) { Text(strings.cancel) }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (playlists.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = strings.noPlaylistsYet,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlists, key = { it.id }) { playlist ->
                    var showMenu by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text(strings.episodeCountLabel(playlist.episodeCount)) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = strings.moreOptionsContentDescription)
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text(strings.rename) },
                                        onClick = { showMenu = false; renamingPlaylist = playlist },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.delete) },
                                        onClick = { showMenu = false; deletingPlaylist = playlist },
                                    )
                                }
                            }
                        },
                        modifier = Modifier.clickable { onOpenPlaylist(playlist.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = strings.newPlaylistContentDescription)
        }
    }
}

@Composable
private fun NamePlaylistDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    strings: AppStrings,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.nameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name); onDismiss() }, enabled = name.isNotBlank()) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
