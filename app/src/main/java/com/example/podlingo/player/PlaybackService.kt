package com.example.podlingo.player

import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.example.podlingo.MainActivity
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.core.TriggerDetector
import com.example.podlingo.core.TriggerResult
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hosts the single ExoPlayer instance and its [MediaSession] so playback keeps running (and stays
 * controllable from the notification shade / lock screen, like Spotify or YouTube Music) even
 * when [com.example.podlingo.MainActivity] isn't in the foreground. [PlayerController] never
 * touches [ExoPlayer] directly - it talks to this service through a [androidx.media3.session.MediaController].
 *
 * Trigger detection (spec section 3) lives here rather than in [PlayerController], because a
 * play/pause request can arrive from the notification or lock screen just as easily as from the
 * in-app button - both route through [MediaSession.Callback.onPlayerCommandRequest] below, so
 * detecting the gesture here is what makes it work from every surface.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var triggerEventBus: TriggerEventBus

    private var mediaSession: MediaSession? = null
    private val triggerDetector = TriggerDetector(thresholdMs = AppDefaults.PAUSE_THRESHOLD_MS)

    override fun onCreate() {
        super.onCreate()
        // ExoPlayer.Builder does NOT request/handle audio focus by default - without this, an
        // incoming call or another app starting playback never paused us, since we never told
        // Android we wanted focus in the first place (so we never got told we lost it).
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                /* handleAudioFocus= */ true,
            )
            .build()
        // Without this, tapping the media notification/lock-screen player does nothing - a
        // MediaSession has no default "open the app" action, it has to be told what to launch.
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(TriggerAwareCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private inner class TriggerAwareCallback : MediaSession.Callback {
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            if (playerCommand != Player.COMMAND_PLAY_PAUSE) return SessionResult.RESULT_SUCCESS
            val player = session.player

            if (player.isPlaying) {
                triggerDetector.onPause(SystemClock.elapsedRealtime(), player.currentPosition)
                return SessionResult.RESULT_SUCCESS
            }

            return when (val result = triggerDetector.onResume(SystemClock.elapsedRealtime())) {
                is TriggerResult.Triggered -> {
                    triggerEventBus.emit(result)
                    // Reject the play - stays paused until PlayerViewModel resumes it once the
                    // trigger's Hebrew narration finishes.
                    SessionResult.RESULT_INFO_SKIPPED
                }
                TriggerResult.NotTriggered -> SessionResult.RESULT_SUCCESS
            }
        }
    }
}
