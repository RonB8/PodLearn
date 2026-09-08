package com.example.podlingo.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.core.WordNormalizer
import com.example.podlingo.core.WordTiming
import com.example.podlingo.data.local.entity.SentenceEntity
import com.example.podlingo.data.local.entity.WordKnowledgeStatus
import com.example.podlingo.data.repository.EpisodeDownload
import com.example.podlingo.data.repository.EpisodeDownloadManager
import com.example.podlingo.data.repository.SettingsRepository
import com.example.podlingo.data.repository.TranscriptRepository
import com.example.podlingo.data.repository.TranslationRepository
import com.example.podlingo.data.repository.WordKnowledgeRepository
import com.example.podlingo.player.PlayerController
import com.example.podlingo.speech.HebrewSpeaker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class NowPlayingUi(
    val episodeId: String,
    val episodeTitle: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
)

/**
 * Activity-scoped (created once, directly inside [com.example.podlingo.ui.navigation.PodLingoNavHost]
 * rather than any single route) so the persistent mini-player bar reflects [PlayerController]'s
 * shared state no matter which screen is on top.
 *
 * Also owns the auto-translate unknown-word watcher while the full Player screen isn't the one on
 * top (see [setPlayerScreenActive]) - that screen's own [PlayerViewModel] runs the identical watcher
 * whenever it's alive, so this one stands down to avoid firing the same word twice. Without this,
 * auto-translate (and its read-aloud pause+TTS) would simply stop the moment the user backs out to
 * the mini-player, since [PlayerViewModel] is cleared with the Player route's back-stack entry.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val transcriptRepository: TranscriptRepository,
    private val wordKnowledgeRepository: WordKnowledgeRepository,
    private val translationRepository: TranslationRepository,
    private val settingsRepository: SettingsRepository,
    private val hebrewSpeaker: HebrewSpeaker,
    episodeDownloadManager: EpisodeDownloadManager,
) : ViewModel() {

    val activeDownloads: StateFlow<List<EpisodeDownload>> = episodeDownloadManager.activeDownloads

    val nowPlaying: StateFlow<NowPlayingUi?> = playerController.playerState
        .map { state ->
            val episodeId = state.episodeId ?: return@map null
            NowPlayingUi(
                episodeId = episodeId,
                episodeTitle = state.episodeTitle.orEmpty(),
                artworkUrl = state.artworkUrl,
                isPlaying = state.isPlaying,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePlayPause() {
        if (playerController.playerState.value.isPlaying) playerController.pause() else playerController.play()
    }

    // --- Auto-translate watcher, only while the full Player screen isn't showing -----------

    private var playerScreenActive = false
    private var watchedEpisodeId: String? = null
    private var watchedSentences: List<SentenceEntity> = emptyList()
    private var unknownWordOccurrences: List<WordTiming> = emptyList()
    private val firedUnknownWords = mutableSetOf<String>()
    private val spokenSentenceIdsForReadAloud = mutableSetOf<String>()

    private val _translationBanner = MutableStateFlow<WordTranslationPopup?>(null)
    val translationBanner: StateFlow<WordTranslationPopup?> = _translationBanner.asStateFlow()

    fun dismissTranslationBanner() {
        _translationBanner.value = null
    }

    /** Called from [com.example.podlingo.ui.navigation.PodLingoNavHost] whenever the current route changes. */
    fun setPlayerScreenActive(active: Boolean) {
        playerScreenActive = active
        if (!active) {
            // Starting a clean slate for the mini-player span - this watcher and PlayerViewModel's
            // own copy aren't kept in sync, so words already fired while the screen was open (or
            // vice versa) simply become eligible again rather than trying to reconcile the two.
            watchedEpisodeId = null
            firedUnknownWords.clear()
            spokenSentenceIdsForReadAloud.clear()
            _translationBanner.value = null
        }
    }

    init {
        viewModelScope.launch {
            playerController.playerState.collect { state ->
                val episodeId = state.episodeId
                if (playerScreenActive || episodeId == null || !settingsRepository.autoTranslateEnabled.value) return@collect
                if (episodeId != watchedEpisodeId) {
                    watchedEpisodeId = episodeId
                    refreshUnknownWordOccurrences(episodeId)
                }
                checkAutoTranslatePopup(state.positionMs)
            }
        }
    }

    private suspend fun refreshUnknownWordOccurrences(episodeId: String) {
        watchedSentences = transcriptRepository.getSentences(episodeId)
        val words = transcriptRepository.getWordTimings(episodeId)
        val deduped = words.distinctBy { WordNormalizer.normalize(it.word) }
        val statuses = wordKnowledgeRepository.getStatuses(deduped.map { it.word })
        unknownWordOccurrences = deduped
            .filter { statuses[WordNormalizer.normalize(it.word)]?.status == WordKnowledgeStatus.UNKNOWN }
            .sortedBy { it.startMs }
    }

    private fun checkAutoTranslatePopup(positionMs: Long) {
        val effectiveTimeMs = positionMs - AppDefaults.REACTION_DELAY_MS
        val currentSentenceId = watchedSentences.lastOrNull { it.startMs <= effectiveTimeMs }?.id ?: return
        val readAloud = settingsRepository.autoTranslateReadAloudEnabled.value
        val sentenceMode = readAloud && !settingsRepository.hardWordModeAutoTranslateEnabled.value
        if (sentenceMode && currentSentenceId in spokenSentenceIdsForReadAloud) return
        val next = unknownWordOccurrences.firstOrNull {
            it.sentenceId == currentSentenceId &&
                it.startMs <= effectiveTimeMs &&
                WordNormalizer.normalize(it.word) !in firedUnknownWords
        } ?: return
        firedUnknownWords += WordNormalizer.normalize(next.word)
        if (readAloud) {
            if (sentenceMode) spokenSentenceIdsForReadAloud += currentSentenceId
            viewModelScope.launch { speakAutoTranslatedWord(next) }
        } else {
            viewModelScope.launch {
                val translation = wordKnowledgeRepository.getOrFetchTranslation(next.word) ?: return@launch
                _translationBanner.value = WordTranslationPopup(next.word, translation)
            }
        }
    }

    private suspend fun speakAutoTranslatedWord(occurrence: WordTiming) {
        // The pause/resume around this narration are the app's own doing, not a user gesture, but
        // PlaybackService's MediaSession callback can't otherwise tell the difference - see
        // TriggerEventBus.suppressTriggerDetection. Without this, a narration that finishes
        // quickly can land inside the trigger's own pause-then-quick-resume window and get misread
        // as the user triggering a fresh (full-sentence, since this isn't the trigger's own
        // hard-word setting) translation on top of the one just read.
        playerController.setSuppressTriggerDetection(true)
        try {
            playerController.pause()
            if (settingsRepository.hardWordModeAutoTranslateEnabled.value) {
                val word = occurrence.word.trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
                val translation = wordKnowledgeRepository.getOrFetchTranslation(word)
                speakEnglishThenHebrew(word, translation)
            } else {
                val sentence = transcriptRepository.getSentence(occurrence.sentenceId)
                if (sentence != null) {
                    val translation = translationRepository.translateToHebrew(sentence.fullText).getOrNull()
                    speakEnglishThenHebrew(sentence.fullText, translation)
                }
            }
            // No quiz overlay can be showing while the mini-player is active (it only ever renders
            // inside the full Player screen), so resuming unconditionally is safe here.
            playerController.resume()
        } finally {
            playerController.setSuppressTriggerDetection(false)
        }
    }

    private suspend fun speakEnglishThenHebrew(englishText: String, translated: String?) {
        withTimeoutOrNull(AppDefaults.TTS_WAIT_TIMEOUT_MS) { hebrewSpeaker.speak(englishText, HebrewSpeaker.ENGLISH) }
        if (translated != null) {
            withTimeoutOrNull(AppDefaults.TTS_WAIT_TIMEOUT_MS) { hebrewSpeaker.speak(translated) }
        }
    }
}
