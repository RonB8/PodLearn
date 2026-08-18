@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.data.repository.PreprocessingProgress
import com.example.podlingo.player.PlayerUiState

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle(uiState)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                PlayerScreenState.Loading -> LoadingView()
                is PlayerScreenState.Preprocessing -> PreprocessingView(state)
                is PlayerScreenState.Ready -> ReadyPlayerView(
                    state = state,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSkipBackward = viewModel::skipBackward,
                    onSkipForward = viewModel::skipForward,
                    onSeek = viewModel::seekTo,
                    onDismissOverlay = viewModel::dismissSentenceOverlay,
                )
                is PlayerScreenState.Failed -> FailedView(state.message)
            }
        }
    }
}

private fun screenTitle(state: PlayerScreenState): String = when (state) {
    is PlayerScreenState.Preprocessing -> state.episodeTitle
    is PlayerScreenState.Ready -> state.episodeTitle
    is PlayerScreenState.Failed -> state.episodeTitle ?: "Episode"
    PlayerScreenState.Loading -> "Episode"
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PreprocessingView(state: PlayerScreenState.Preprocessing) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = preprocessingLabel(state.progress), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        val fraction = (state.progress as? PreprocessingProgress.Downloading)?.fraction
        if (fraction != null && fraction >= 0f) {
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun preprocessingLabel(progress: PreprocessingProgress): String = when (progress) {
    is PreprocessingProgress.Downloading -> "Downloading episode..."
    PreprocessingProgress.Transcribing -> "Transcribing speech (this can take a while)..."
    PreprocessingProgress.Processing -> "Building transcript..."
    PreprocessingProgress.Ready -> "Ready"
    is PreprocessingProgress.Failed -> progress.message
}

@Composable
private fun FailedView(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReadyPlayerView(
    state: PlayerScreenState.Ready,
    onTogglePlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismissOverlay: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            EpisodeArtwork()
        }
        Spacer(modifier = Modifier.height(32.dp))
        PlaybackControls(
            player = state.player,
            onTogglePlayPause = onTogglePlayPause,
            onSkipBackward = onSkipBackward,
            onSkipForward = onSkipForward,
            onSeek = onSeek,
        )

        val showOverlay = state.resolvedSentenceText != null || state.noRelevantSentence
        AnimatedVisibility(visible = showOverlay) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                SentenceOverlay(
                    text = state.resolvedSentenceText,
                    translatedText = state.translatedSentenceText,
                    isTranslating = state.isTranslating,
                    onDismiss = onDismissOverlay,
                )
            }
        }
    }
}

@Composable
private fun EpisodeArtwork() {
    Box(
        modifier = Modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun PlaybackControls(
    player: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf(0f) }
    val durationMs = player.durationMs.coerceAtLeast(1L)
    val sliderValue = (if (isDragging) dragPositionMs else player.positionMs.toFloat()).coerceIn(0f, durationMs.toFloat())

    Slider(
        value = sliderValue,
        onValueChange = {
            isDragging = true
            dragPositionMs = it
        },
        onValueChangeFinished = {
            onSeek(dragPositionMs.toLong())
            isDragging = false
        },
        valueRange = 0f..durationMs.toFloat(),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatMillis(if (isDragging) dragPositionMs.toLong() else player.positionMs))
        Text(formatMillis(player.durationMs))
    }
    Spacer(modifier = Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        SkipButton(seconds = AppDefaults.SEEK_STEP_MS / 1000, isForward = false, onClick = onSkipBackward)

        if (player.isBuffering) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            FilledIconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) {
                Icon(
                    imageVector = if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (player.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        SkipButton(seconds = AppDefaults.SEEK_STEP_MS / 1000, isForward = true, onClick = onSkipForward)
    }
}

@Composable
private fun SkipButton(seconds: Long, isForward: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Replay,
                contentDescription = if (isForward) "Skip forward $seconds seconds" else "Skip back $seconds seconds",
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer { if (isForward) scaleX = -1f },
            )
            Text(
                text = seconds.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SentenceOverlay(
    text: String?,
    translatedText: String?,
    isTranslating: Boolean,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (text != null) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = text, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    when {
                        translatedText != null -> Text(
                            text = translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        isTranslating -> Text(
                            text = "Translating…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                Text(
                    text = "No relevant sentence found (that pause looks like it fell in a quiet stretch).",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss")
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
