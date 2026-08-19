@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.core.WordTiming
import com.example.podlingo.data.local.entity.SentenceEntity
import com.example.podlingo.data.repository.PreprocessingProgress
import com.example.podlingo.player.PlayerUiState
import com.example.podlingo.ui.playlists.AddToPlaylistDialog

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onNavigateToEpisode: (String) -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddToPlaylist by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateToEpisode.collect { episodeId -> onNavigateToEpisode(episodeId) }
    }

    val readyState = uiState as? PlayerScreenState.Ready
    if (showAddToPlaylist && readyState != null) {
        AddToPlaylistDialog(episodeId = readyState.episodeId, onDismiss = { showAddToPlaylist = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle(uiState)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (readyState != null) {
                        IconButton(onClick = { showAddToPlaylist = true }) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Add to playlist")
                        }
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
                    onSkipToNextEpisode = viewModel::skipToNextEpisode,
                    onSpeedSelected = viewModel::setPlaybackSpeed,
                    onSeek = viewModel::seekTo,
                    onDismissOverlay = viewModel::dismissSentenceOverlay,
                    onSwipeDownDismiss = onBack,
                    onToggleTranscript = viewModel::toggleTranscript,
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
    is PreprocessingProgress.Transcribing -> if (progress.chunkCount > 1) {
        "Transcribing speech (part ${progress.chunkIndex}/${progress.chunkCount})..."
    } else {
        "Transcribing speech (this can take a while)..."
    }
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
    onSkipToNextEpisode: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onDismissOverlay: () -> Unit,
    onSwipeDownDismiss: () -> Unit,
    onToggleTranscript: () -> Unit,
) {
    val wordsBySentence = remember(state.words) { state.words.groupBy { it.sentenceId } }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            EpisodeArtwork(
                artworkUrl = state.artworkUrl,
                transcriptVisible = state.transcriptVisible,
                sentences = state.sentences,
                wordsBySentence = wordsBySentence,
                positionMs = state.player.positionMs,
                activeSentenceId = state.activeSentenceId,
                activeWord = state.activeWord,
                isTranslating = state.isTranslating,
                translatedSentenceText = state.translatedSentenceText,
                onDismissOverlay = onDismissOverlay,
                onSwipeDownDismiss = onSwipeDownDismiss,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TranscriptButtonRow(transcriptVisible = state.transcriptVisible, onToggleTranscript = onToggleTranscript)
        Spacer(modifier = Modifier.height(20.dp))
        PlaybackControls(
            player = state.player,
            hasNextEpisode = state.hasNextEpisode,
            onTogglePlayPause = onTogglePlayPause,
            onSkipBackward = onSkipBackward,
            onSkipForward = onSkipForward,
            onSkipToNextEpisode = onSkipToNextEpisode,
            onSpeedSelected = onSpeedSelected,
            onSeek = onSeek,
        )

        // The translation itself now lives inside the transcript overlay (see TranslationPanel) -
        // this banner only covers the edge case where no sentence lines up with the pause at all,
        // since there's no sentence in the transcript to attach that message to.
        AnimatedVisibility(visible = state.noRelevantSentence) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                NoRelevantSentenceBanner(onDismiss = onDismissOverlay)
            }
        }
    }
}

/**
 * Dragging down past [SWIPE_DISMISS_THRESHOLD_DP] triggers [onSwipeDownDismiss] - the artwork is
 * the one large area on this screen with no other gesture (the slider below drags horizontally,
 * buttons only tap), so it's the safe, conflict-free target for the "pull down to dismiss" swipe
 * standard in media players like Spotify.
 */
@Composable
private fun EpisodeArtwork(
    artworkUrl: String?,
    transcriptVisible: Boolean,
    sentences: List<SentenceEntity>,
    wordsBySentence: Map<String, List<WordTiming>>,
    positionMs: Long,
    activeSentenceId: String?,
    activeWord: String?,
    isTranslating: Boolean,
    translatedSentenceText: String?,
    onDismissOverlay: () -> Unit,
    onSwipeDownDismiss: () -> Unit,
) {
    val dismissThresholdPx = with(LocalDensity.current) { SWIPE_DISMISS_THRESHOLD_DP.dp.toPx() }
    var accumulatedDrag by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            // Swiping to scroll the transcript and swiping to dismiss both read vertical drags on
            // this same box, so the dismiss gesture only listens while the transcript is hidden.
            .let { base ->
                if (transcriptVisible) {
                    base
                } else {
                    base.draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> accumulatedDrag += delta },
                        onDragStopped = {
                            if (accumulatedDrag > dismissThresholdPx) onSwipeDownDismiss()
                            accumulatedDrag = 0f
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Kept underneath as a fallback: shows through if there's no artwork, or it fails to load.
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        AnimatedVisibility(visible = transcriptVisible, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.68f)),
            ) {
                TranscriptView(
                    sentences = sentences,
                    wordsBySentence = wordsBySentence,
                    positionMs = positionMs,
                    activeSentenceId = activeSentenceId,
                    activeWord = activeWord,
                    isTranslating = isTranslating,
                    translatedSentenceText = translatedSentenceText,
                    onDismissOverlay = onDismissOverlay,
                )
            }
        }
    }
}

@Composable
private fun TranscriptButtonRow(transcriptVisible: Boolean, onToggleTranscript: () -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = transcriptVisible,
                onClick = onToggleTranscript,
                label = { Text("Transcript") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
        }
    }
}

/**
 * Spotify-style synced transcript: while no trigger is active, the sentence/word containing the
 * live playback position is highlighted (karaoke-style, regular bold); once a trigger fires, that
 * takes over with a heavier bold - the whole sentence for normal mode, or just the target word for
 * hard-word mode - and a [TranslationPanel] takes the place of the sentences after it, so the
 * translation the user is actively hearing is never competing for attention with what's next.
 */
@Composable
private fun TranscriptView(
    sentences: List<SentenceEntity>,
    wordsBySentence: Map<String, List<WordTiming>>,
    positionMs: Long,
    activeSentenceId: String?,
    activeWord: String?,
    isTranslating: Boolean,
    translatedSentenceText: String?,
    onDismissOverlay: () -> Unit,
) {
    val listState = rememberLazyListState()
    val highlightedSentenceId = activeSentenceId
        ?: sentences.lastOrNull { it.startMs <= positionMs }?.id
    val showTranslationPanel = activeSentenceId != null && (isTranslating || translatedSentenceText != null)
    val highlightedIndex = sentences.indexOfFirst { it.id == highlightedSentenceId }
    val visibleSentences = if (showTranslationPanel && highlightedIndex >= 0) {
        sentences.subList(0, highlightedIndex + 1)
    } else {
        sentences
    }

    LaunchedEffect(highlightedSentenceId) {
        val index = sentences.indexOfFirst { it.id == highlightedSentenceId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(visibleSentences, key = { it.id }) { sentence ->
            val isCurrentSentence = sentence.id == highlightedSentenceId
            val words = wordsBySentence[sentence.id].orEmpty()
            // Trigger-driven (an active translation) gets a heavier weight than plain time-based
            // karaoke tracking, so the word/sentence actually being translated stands out from
            // ordinary "this is where we are" highlighting.
            val isTriggerDriven = isCurrentSentence && activeSentenceId == sentence.id
            val boldWholeSentence = isTriggerDriven && activeWord == null
            val highlightedWord: WordTiming? = when {
                !isCurrentSentence -> null
                isTriggerDriven && activeWord != null -> words.firstOrNull { wordMatchesActiveWord(it.word, activeWord) }
                !isTriggerDriven -> words.firstOrNull { positionMs in it.startMs until it.endMs }
                else -> null
            }
            Text(
                text = buildSentenceAnnotatedString(
                    sentence = sentence,
                    words = words,
                    isCurrentSentence = isCurrentSentence,
                    boldWholeSentence = boldWholeSentence,
                    highlightedWord = highlightedWord,
                    highlightWeight = if (isTriggerDriven) FontWeight.Black else FontWeight.Bold,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        if (showTranslationPanel) {
            item(key = "translation-panel") {
                TranslationPanel(
                    isTranslating = isTranslating,
                    translatedText = translatedSentenceText,
                    onDismiss = onDismissOverlay,
                )
            }
        }
    }
}

/** [activeWord] (from hard-word mode) is punctuation-trimmed but not lowercased, so match loosely. */
private fun wordMatchesActiveWord(rawWord: String, activeWord: String): Boolean {
    val trimmed = rawWord.trim { c -> !c.isLetterOrDigit() && c != '\'' && c != '-' }
    return trimmed.equals(activeWord, ignoreCase = true)
}

private fun buildSentenceAnnotatedString(
    sentence: SentenceEntity,
    words: List<WordTiming>,
    isCurrentSentence: Boolean,
    boldWholeSentence: Boolean,
    highlightedWord: WordTiming?,
    highlightWeight: FontWeight,
): AnnotatedString {
    val alpha = if (isCurrentSentence) 1f else 0.45f
    return buildAnnotatedString {
        if (words.isEmpty()) {
            withStyle(SpanStyle(color = Color.White.copy(alpha = alpha))) { append(sentence.fullText) }
            return@buildAnnotatedString
        }
        words.forEachIndexed { index, word ->
            // Reference equality (not text equality) so a repeated word elsewhere in the sentence
            // never gets bolded by mistake.
            val isHighlighted = boldWholeSentence || word === highlightedWord
            withStyle(
                SpanStyle(
                    color = Color.White.copy(alpha = alpha),
                    fontWeight = if (isHighlighted) highlightWeight else FontWeight.Normal,
                ),
            ) {
                append(word.word)
            }
            if (index != words.lastIndex) append(" ")
        }
    }
}

/**
 * Replaces the transcript's upcoming sentences while a trigger's translation is active - it's
 * deliberately opaque (not just a scrim) so it reads as "this is what you're hearing right now",
 * covering rather than competing with what comes next in the episode.
 */
@Composable
private fun TranslationPanel(isTranslating: Boolean, translatedText: String?, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.94f))
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (translatedText != null) {
                Text(
                    text = translatedText,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = "Translating…",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    player: PlayerUiState,
    hasNextEpisode: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSkipToNextEpisode: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
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
    // The core back/play/forward group is centered as its own unit (Box + align(Center)) so the
    // play button's position never shifts based on whether the optional next-episode button (a
    // separate, unrelated action) happens to be showing - it docks at the end instead.
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
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

        SpeedButton(
            currentSpeed = player.playbackSpeed,
            onSpeedSelected = onSpeedSelected,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        if (hasNextEpisode) {
            IconButton(
                onClick = onSkipToNextEpisode,
                modifier = Modifier.align(Alignment.CenterEnd).size(52.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next episode",
                    modifier = Modifier.size(32.dp),
                )
            }
        }
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
private fun NoRelevantSentenceBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "No relevant sentence found (that pause looks like it fell in a quiet stretch).",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
private fun SpeedButton(currentSpeed: Float, onSpeedSelected: (Float) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(onClick = { expanded = true }) {
            Text(formatSpeed(currentSpeed))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SPEED_OPTIONS.forEach { speed ->
                DropdownMenuItem(
                    text = { Text(formatSpeed(speed)) },
                    onClick = { onSpeedSelected(speed); expanded = false },
                )
            }
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}.0x" else "${speed}x"

private val SPEED_OPTIONS = listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private const val SWIPE_DISMISS_THRESHOLD_DP = 96
