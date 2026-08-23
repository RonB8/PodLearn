package com.example.podlingo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.podlingo.ui.common.ArtworkThumbnail

private const val SWIPE_UP_THRESHOLD_DP = 24

@Composable
fun MiniPlayerBar(
    nowPlaying: NowPlayingUi,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (nowPlaying.durationMs > 0) {
        (nowPlaying.positionMs.toFloat() / nowPlaying.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val swipeUpThresholdPx = with(LocalDensity.current) { SWIPE_UP_THRESHOLD_DP.dp.toPx() }
    var accumulatedDrag by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 3.dp,
    ) {
        // Content is padded above the system nav bar/gesture strip so the title and play button
        // stay tappable; the colored Surface background still extends behind it edge-to-edge.
        Column(modifier = Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    // A small upward flick also opens the player - the natural "pull this thing
                    // up" gesture Spotify/YouTube Music users already expect from a mini-player.
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> accumulatedDrag += delta },
                        onDragStopped = {
                            if (accumulatedDrag < -swipeUpThresholdPx) onClick()
                            accumulatedDrag = 0f
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkThumbnail(artworkUrl = nowPlaying.artworkUrl, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = nowPlaying.episodeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (nowPlaying.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (nowPlaying.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
