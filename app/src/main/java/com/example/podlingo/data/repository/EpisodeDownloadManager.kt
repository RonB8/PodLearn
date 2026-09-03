package com.example.podlingo.data.repository

import com.example.podlingo.data.local.entity.EpisodeEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One episode's live preprocessing state, as tracked by [EpisodeDownloadManager] - for UI that lists every download in flight, not just one episode's own screen. */
data class EpisodeDownload(
    val episodeId: String,
    val episodeTitle: String,
    val progress: PreprocessingProgress,
)

/**
 * Runs [TranscriptRepository.preprocess] (download + transcribe) on a coroutine scope of its
 * own rather than a screen's `viewModelScope`, so navigating away from the Player - even all
 * the way back to Home - no longer cancels an in-flight download. At most one preprocessing run
 * is active per episode at a time: a second [progressFor] call for the same episode while one is
 * already running shares that run's progress instead of starting a duplicate download.
 */
@Singleton
class EpisodeDownloadManager @Inject constructor(
    private val transcriptRepository: TranscriptRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Only touched from the Main dispatcher (the scope above, and every progressFor caller), so a
    // plain MutableMap is safe without extra synchronization.
    private val runs = mutableMapOf<String, MutableStateFlow<PreprocessingProgress?>>()

    private val _activeDownloads = MutableStateFlow<List<EpisodeDownload>>(emptyList())
    /** Every episode currently downloading or transcribing, for a persistent app-wide indicator. Episodes that only need a quick "is it already Ready" check never appear here - see the Ready/Failed branch below. */
    val activeDownloads: StateFlow<List<EpisodeDownload>> = _activeDownloads.asStateFlow()

    fun progressFor(episode: EpisodeEntity): StateFlow<PreprocessingProgress?> {
        runs[episode.id]?.let { return it.asStateFlow() }

        val progress = MutableStateFlow<PreprocessingProgress?>(null)
        runs[episode.id] = progress
        scope.launch {
            transcriptRepository.preprocess(episode).collect { update ->
                progress.value = update
                _activeDownloads.update { list ->
                    when (update) {
                        is PreprocessingProgress.Ready, is PreprocessingProgress.Failed ->
                            list.filterNot { it.episodeId == episode.id }
                        else -> {
                            val entry = EpisodeDownload(episode.id, episode.title, update)
                            if (list.any { it.episodeId == episode.id }) {
                                list.map { if (it.episodeId == episode.id) entry else it }
                            } else {
                                list + entry
                            }
                        }
                    }
                }
            }
            runs.remove(episode.id)
        }
        return progress.asStateFlow()
    }
}
