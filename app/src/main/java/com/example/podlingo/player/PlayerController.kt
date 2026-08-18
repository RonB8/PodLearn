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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

/**
 * Client-side wrapper around a [MediaController] connected to [PlaybackService], which is what
 * actually owns the ExoPlayer instance. Routing playback through the service (instead of holding
 * an ExoPlayer directly) is what gives us a system media notification and lock-screen transport
 * controls, matching how Spotify/YouTube Music behave. Trigger detection (spec section 3) happens
 * server-side in [PlaybackService] - see [triggerEvents] - since it must react to play/pause from
 * any source, not just this controller's own [play]/[pause] calls. One instance is owned per
 * player screen (created fresh per PlayerViewModel, released in its onCleared) - not an app-wide
 * singleton.
 */
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val triggerEventBus: TriggerEventBus,
) {

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    val triggerEvents: SharedFlow<TriggerResult.Triggered> = triggerEventBus.events

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
        }
    }

    fun prepare(episodeTitle: String, localFilePath: String) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val mediaController = future.get()
                controller = mediaController
                mediaController.addListener(playerListener)
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.fromFile(File(localFilePath)))
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(episodeTitle).build())
                    .build()
                mediaController.setMediaItem(mediaItem)
                mediaController.prepare()
                mediaController.play()
            },
            MoreExecutors.directExecutor(),
        )
    }

    /**
     * User-initiated play/resume. If this resume is a trigger gesture, [PlaybackService] rejects
     * the underlying play (see its `MediaSession.Callback`) and playback stays paused until the
     * caller resumes it via [resume] once the trigger's Hebrew narration finishes.
     */
    fun play() {
        controller?.play()
    }

    /** Resumes actual playback once the trigger's narration has finished (or been dismissed). */
    fun resume() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun seekForward(deltaMs: Long = AppDefaults.SEEK_STEP_MS) {
        val mediaController = controller ?: return
        val duration = mediaController.duration.coerceAtLeast(0)
        val target = (mediaController.currentPosition + deltaMs).let { if (duration > 0) it.coerceAtMost(duration) else it }
        mediaController.seekTo(target)
    }

    fun seekBackward(deltaMs: Long = AppDefaults.SEEK_STEP_MS) {
        val mediaController = controller ?: return
        val target = (mediaController.currentPosition - deltaMs).coerceAtLeast(0)
        mediaController.seekTo(target)
    }

    fun release() {
        stopPositionUpdates()
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
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
