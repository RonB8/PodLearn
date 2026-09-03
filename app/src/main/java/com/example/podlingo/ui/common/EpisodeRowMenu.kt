package com.example.podlingo.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.podlingo.ui.strings.LocalAppStrings

/**
 * The three-dot overflow menu on an episode row - Add to playlist / Quiz / Delete - shared by
 * every episode list in the app (History, Saved, a podcast's episode list, a playlist) so they
 * all behave and read identically. [deleteEnabled] lets a caller grey out Delete rather than hide
 * it, e.g. an episode row with nothing downloaded to remove yet.
 */
@Composable
fun EpisodeRowMenu(
    onAddToPlaylist: () -> Unit,
    onQuiz: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleteEnabled: Boolean = true,
) {
    val strings = LocalAppStrings.current
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = strings.moreOptionsContentDescription)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(strings.addToPlaylist) },
                onClick = { showMenu = false; onAddToPlaylist() },
            )
            DropdownMenuItem(
                text = { Text(strings.quizMenuItem) },
                onClick = { showMenu = false; onQuiz() },
            )
            DropdownMenuItem(
                text = { Text(strings.delete) },
                onClick = { showMenu = false; onDelete() },
                enabled = deleteEnabled,
            )
        }
    }
}
