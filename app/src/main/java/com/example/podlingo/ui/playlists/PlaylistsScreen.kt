@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.data.local.dao.PlaylistWithCount

@Composable
fun PlaylistsScreen(
    onOpenPlaylist: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var renamingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistWithCount?>(null) }

    if (showCreateDialog) {
        NamePlaylistDialog(
            title = "New playlist",
            initialName = "",
            onConfirm = { name -> viewModel.createPlaylist(name) },
            onDismiss = { showCreateDialog = false },
        )
    }
    renamingPlaylist?.let { playlist ->
        NamePlaylistDialog(
            title = "Rename playlist",
            initialName = playlist.name,
            onConfirm = { name -> viewModel.renamePlaylist(playlist.id, name) },
            onDismiss = { renamingPlaylist = null },
        )
    }
    deletingPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            title = { Text("Delete \"${playlist.name}\"?") },
            text = { Text("This removes the playlist. The episodes in it aren't affected.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlaylist(playlist.id); deletingPlaylist = null }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlaylist = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlists") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New playlist")
            }
        },
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No playlists yet. Tap + to create one.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(playlists, key = { it.id }) { playlist ->
                    var showMenu by remember { mutableStateOf(false) }
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text(episodeCountLabel(playlist.episodeCount)) },
                        trailingContent = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = { showMenu = false; renamingPlaylist = playlist },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
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
    }
}

@Composable
private fun NamePlaylistDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name); onDismiss() }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun episodeCountLabel(count: Int): String = if (count == 1) "1 episode" else "$count episodes"
