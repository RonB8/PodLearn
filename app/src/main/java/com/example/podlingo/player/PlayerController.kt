package com.example.podlingo.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.core.TriggerResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlayerUiState(
    val episodeId: String? = null,
    val episodeTitle: String? = null,
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
)

/**
 * Client-side wrapper around a [MediaController] connected to [PlaybackService], which is what
 * actually owns the ExoPlayer instance. Routing playback through the service (instead of holding
 * an ExoPlayer directly) is what gives us a system media notification and lock-screen transport
 * controls, matching how Spotify/YouTube Music behave. Trigger detection (spec section 3) happens
 * server-side in [PlaybackService] - see [triggerEvents] - since it must react to play/pause from
 * any source, not just this controller's own [play]/[pause] calls.
 *
 * App-wide singleton, not per-screen: the persistent mini-player bar (shown on every screen except
 * the full Player screen) and [com.example.podlingo.ui.player.PlayerViewModel] both observe the
 * same connection, so leaving the Player screen must not tear it down - the whole point is that
 * playback (and the ability to see/control it) survives navigating away.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val triggerEventBus: TriggerEventBus,
) {

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    /** Emits the episode id whenever playback runs to the end of that episode's audio. */
    private val _playbackEnded = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val playbackEnded: SharedFlow<String> = _playbackEnded.asSharedFlow()

    val triggerEvents: SharedFlow<TriggerResult.Triggered> = triggerEventBus.events

    /** See [TriggerEventBus.previousSentenceRequests]. */
    val previousSentenceRequests: SharedFlow<Unit> = triggerEventBus.previousSentenceRequests

    /** See [TriggerEventBus.translationOverlayActive]. */
    fun setTranslationOverlayActive(active: Boolean) {
        triggerEventBus.translationOverlayActive = active
    }

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val positionUpdateHandler = Handler(Looper.getMainLooper())
    private var positionUpdateRunnable: Runnable? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val duration = controller?.duration?.coerceAtLeast(0) ?: 0L
            _playerState.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    durationMs = duration,
                )
            }
            if (playbackState == Player.STATE_ENDED) {
                _playerState.value.episodeId?.let { _playbackEnded.tryEmit(it) }
            }
        }
    }

    /**
     * Loads [episodeId]'s audio and starts playback - unless it's already the loaded episode
     * (e.g. re-entering the Player screen for something mid-playback), in which case this is a
     * no-op so playback isn't restarted from the top. [autoPlay] false loads the audio (ready to
     * play the instant something calls [play]/[resume]) without actually starting it - for a
     * fresh episode with a start-quiz prompt still pending, so the audio doesn't play out from
     * under the prompt even for a moment.
     */
    fun prepare(episodeId: String, episodeTitle: String, artworkUrl: String?, localFilePath: String, autoPlay: Boolean = true) {
        if (_playerState.value.episodeId == episodeId) return
        withController { mediaController ->
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(File(localFilePath)))
                .setMediaMetadata(MediaMetadata.Builder().setTitle(episodeTitle).build())
                .build()
            val speed = _playerState.value.playbackSpeed
            _playerState.value = PlayerUiState(
                episodeId = episodeId,
                episodeTitle = episodeTitle,
                artworkUrl = artworkUrl,
                playbackSpeed = speed,
            )
            // Must be set before setMediaItem/prepare(), not decided afterwards via a conditional
            // play() call - the controller may still have playWhenReady=true left over from
            // whatever was playing before (this is an app-wide singleton session), in which case
            // loading a new item would auto-start it regardless of autoPlay.
            mediaController.playWhenReady = autoPlay
            mediaController.setMediaItem(mediaItem)
            mediaController.prepare()
            mediaController.setPlaybackSpeed(speed)
        }
    }

    /** Applies to whatever's currently loaded and carries over to the next episode too. */
    fun setPlaybackSpeed(speed: Float) = withController { mediaController ->
        mediaController.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    /**
     * User-initiated play/resume. If this resume is a trigger gesture, [PlaybackService] rejects
     * the underlying play (see its `MediaSession.Callback`) and playback stays paused until the
     * caller resumes it via [resume] once the trigger's Hebrew narration finishes.
     */
    fun play() = withController { it.play() }

    /** Resumes actual playback once the trigger's narration has finished (or been dismissed). */
    fun resume() = withController { it.play() }

    fun pause() = withController { it.pause() }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    fun seekForward(deltaMs: Long = AppDefaults.SEEK_STEP_MS) = withController { mediaController ->
        val duration = mediaController.duration.coerceAtLeast(0)
        val target = (mediaController.currentPosition + deltaMs).let { if (duration > 0) it.coerceAtMost(duration) else it }
        mediaController.seekTo(target)
    }

    fun seekBackward(deltaMs: Long = AppDefaults.SEEK_STEP_MS) = withController { mediaController ->
        val target = (mediaController.currentPosition - deltaMs).coerceAtLeast(0)
        mediaController.seekTo(target)
    }

    /** Connects to [PlaybackService] on first use; queues [action] until that connection is ready. */
    private fun withController(action: (MediaController) -> Unit) {
        controller?.let { action(it); return }
        val future = controllerFuture ?: run {
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            MediaController.Builder(context, sessionToken).buildAsync().also { controllerFuture = it }
        }
        future.addListener(
            {
                val mediaController = controller ?: future.get().also { newController ->
                    controller = newController
                    newController.addListener(playerListener)
                }
                action(mediaController)
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        val runnable = object : Runnable {
            override fun run() {
                _playerState.update { it.copy(positionMs = controller?.currentPosition ?: it.positionMs) }
                positionUpdateHandler.postDelayed(this, POSITION_UPDATE_INTERVAL_MS)
            }
        }
        positionUpdateRunnable = runnable
        positionUpdateHandler.post(runnable)
    }

    private fun stopPositionUpdates() {
        positionUpdateRunnable?.let(positionUpdateHandler::removeCallbacks)
        positionUpdateRunnable = null
    }

    companion object {
        private const val POSITION_UPDATE_INTERVAL_MS = 200L
    }
}
