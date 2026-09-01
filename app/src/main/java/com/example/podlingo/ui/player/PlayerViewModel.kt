package com.example.podlingo.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.core.SentenceResolution
import com.example.podlingo.core.SentenceResolver
import com.example.podlingo.core.WordDifficultyRanker
import com.example.podlingo.core.WordTiming
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.TranscriptStatus
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.data.repository.PreprocessingProgress
import com.example.podlingo.data.repository.SettingsRepository
import com.example.podlingo.data.repository.TranscriptRepository
import com.example.podlingo.data.repository.TranslationRepository
import com.example.podlingo.data.repository.WordDifficultyRepository
import com.example.podlingo.player.PlayerController
import com.example.podlingo.speech.HebrewSpeaker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@HiltViewModel
class PlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val podcastRepository: PodcastRepository,
    private val transcriptRepository: TranscriptRepository,
    private val translationRepository: TranslationRepository,
    private val playerController: PlayerController,
    private val hebrewSpeaker: HebrewSpeaker,
    private val settingsRepository: SettingsRepository,
    private val wordDifficultyRepository: WordDifficultyRepository,
) : ViewModel() {

    private val episodeId: String = checkNotNull(savedStateHandle["episodeId"])

    private val _uiState = MutableStateFlow<PlayerScreenState>(PlayerScreenState.Loading)
    val uiState: StateFlow<PlayerScreenState> = _uiState.asStateFlow()

    /** One-shot: the screen navigates to this episode (manual skip, or auto-advance at episode end). */
    private val _navigateToEpisode = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToEpisode: SharedFlow<String> = _navigateToEpisode.asSharedFlow()

    private var nextEpisodeId: String? = null

    private var cachedWords: List<WordTiming>? = null

    // Progressive hard-word mode: which sentence we're stepping through and how far.
    private var hardWordSentenceId: String? = null
    private var hardWordIndex: Int = 0

    init {
        viewModelScope.launch {
            val episode = podcastRepository.getEpisode(episodeId)
            if (episode == null) {
                _uiState.value = PlayerScreenState.Failed("Episode not found")
                return@launch
            }
            if (episode.transcriptStatus == TranscriptStatus.READY) {
                startPlayback(episode)
            } else {
                runPreprocessing(episode)
            }
        }

        viewModelScope.launch {
            playerController.playerState.collect { playerUiState ->
                _uiState.update { current ->
                    if (current is PlayerScreenState.Ready) current.copy(player = playerUiState) else current
                }
            }
        }

        viewModelScope.launch {
            playerController.triggerEvents.collect { trigger ->
                handleTrigger(trigger.pauseTimeMs)
            }
        }

        viewModelScope.launch {
            playerController.playbackEnded.collect { endedEpisodeId ->
                if (endedEpisodeId != episodeId) return@collect
                val next = nextEpisodeId ?: return@collect
                if (settingsRepository.autoPlayNextEnabled.value) _navigateToEpisode.tryEmit(next)
            }
        }

        viewModelScope.launch {
            settingsRepository.hardWordModeEnabled.collect { enabled ->
                _uiState.update { current ->
                    if (current is PlayerScreenState.Ready) current.copy(hardWordModeEnabled = enabled) else current
                }
            }
        }

    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state !is PlayerScreenState.Ready) return
        if (state.player.isPlaying) playerController.pause() else playerController.play()
    }

    fun skipToNextEpisode() {
        nextEpisodeId?.let { _navigateToEpisode.tryEmit(it) }
    }

    fun skipForward() = playerController.seekForward()

    fun skipBackward() = playerController.seekBackward()

    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)

    fun toggleTranscript() {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(transcriptVisible = !current.transcriptVisible)
        }
    }

    fun toggleHardWordMode() {
        settingsRepository.setHardWordModeEnabled(!settingsRepository.hardWordModeEnabled.value)
    }

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun dismissSentenceOverlay() {
        hebrewSpeaker.stop()
        playerController.resume()
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) {
                current.copy(
                    resolvedSentenceText = null,
                    translatedSentenceText = null,
                    isTranslating = false,
                    noRelevantSentence = false,
                    activeSentenceId = null,
                    activeWord = null,
                )
            } else {
                current
            }
        }
    }

    private suspend fun runPreprocessing(episode: EpisodeEntity) {
        transcriptRepository.preprocess(episode).collect { progress ->
            when (progress) {
                is PreprocessingProgress.Ready -> {
                    val refreshed = podcastRepository.getEpisode(episodeId) ?: episode
                    startPlayback(refreshed)
                }
                is PreprocessingProgress.Failed -> {
                    _uiState.value = PlayerScreenState.Failed(progress.message, episode.title)
                }
                else -> {
                    _uiState.value = PlayerScreenState.Preprocessing(episode.title, progress)
                }
            }
        }
    }

    private suspend fun startPlayback(episode: EpisodeEntity) {
        val localFilePath = episode.localFilePath
        if (localFilePath == null) {
            _uiState.value = PlayerScreenState.Failed("Downloaded audio file is missing")
            return
        }
        val artworkUrl = podcastRepository.getPodcast(episode.podcastId)?.imageUrl
        playerController.prepare(episode.id, episode.title, artworkUrl, localFilePath)
        val podcastEpisodes = podcastRepository.getEpisodes(episode.podcastId).first()
        val currentIndex = podcastEpisodes.indexOfFirst { it.id == episode.id }
        nextEpisodeId = if (currentIndex == -1) null else podcastEpisodes.getOrNull(currentIndex + 1)?.id
        val words = transcriptRepository.getWordTimings(episode.id).also { cachedWords = it }
        val sentences = transcriptRepository.getSentences(episode.id)
        _uiState.value = PlayerScreenState.Ready(
            episodeId = episode.id,
            episodeTitle = episode.title,
            player = playerController.playerState.value,
            artworkUrl = artworkUrl,
            hasNextEpisode = nextEpisodeId != null,
            sentences = sentences,
            words = words,
            hardWordModeEnabled = settingsRepository.hardWordModeEnabled.value,
        )
        podcastRepository.recordEpisodePlayed(episode.id)
    }

    private fun handleTrigger(pauseTimeMs: Long) {
        // A trigger firing means there's a translation to show - surface it in the transcript
        // (covering the upcoming sentences) rather than relying on the user having it open already.
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) current.copy(transcriptVisible = true) else current
        }
        viewModelScope.launch {
            val words = cachedWords ?: transcriptRepository.getWordTimings(episodeId).also { cachedWords = it }
            val effectiveTimeMs = pauseTimeMs - AppDefaults.REACTION_DELAY_MS
            when (val resolution = SentenceResolver.resolve(pauseTimeMs, AppDefaults.REACTION_DELAY_MS, words)) {
                is SentenceResolution.Resolved -> {
                    val sentence = transcriptRepository.getSentence(resolution.sentenceId)
                    if (sentence == null) {
                        _uiState.update { current ->
                            if (current is PlayerScreenState.Ready) {
                                current.copy(
                                    resolvedSentenceText = null,
                                    translatedSentenceText = null,
                                    isTranslating = false,
                                    noRelevantSentence = true,
                                    activeSentenceId = null,
                                    activeWord = null,
                                )
                            } else {
                                current
                            }
                        }
                        playerController.resume()
                        return@launch
                    }
                    if (settingsRepository.hardWordModeEnabled.value) {
                        handleHardWordTrigger(resolution.sentenceId, sentence.fullText, words, effectiveTimeMs)
                    } else {
                        // Leaving hard-word mode's per-sentence progression - restart clean if
                        // it's ever re-entered on this sentence.
                        hardWordSentenceId = null
                        _uiState.update { current ->
                            if (current !is PlayerScreenState.Ready) return@update current
                            current.copy(
                                resolvedSentenceText = sentence.fullText,
                                translatedSentenceText = null,
                                isTranslating = true,
                                noRelevantSentence = false,
                                activeSentenceId = sentence.id,
                                activeWord = null,
                            )
                        }
                        translateAndSpeak(sentence.fullText)
                    }
                }
                SentenceResolution.NoRelevantSentence -> {
                    _uiState.update { current ->
                        if (current is PlayerScreenState.Ready) {
                            current.copy(
                                resolvedSentenceText = null,
                                translatedSentenceText = null,
                                isTranslating = false,
                                noRelevantSentence = true,
                                activeSentenceId = null,
                                activeWord = null,
                            )
                        } else {
                            current
                        }
                    }
                    playerController.resume()
                }
            }
        }
    }

    /**
     * Hard-word mode: translate only the sentence's hardest word (by Oxford CEFR level). An
     * immediate re-trigger on the *same* sentence steps to the next-hardest word instead of
     * restarting; a trigger elsewhere starts a fresh sentence at its hardest word. The cycle
     * wraps back to the hardest word once every word has been shown.
     */
    private suspend fun handleHardWordTrigger(
        sentenceId: String,
        sentenceText: String,
        allWords: List<WordTiming>,
        effectiveTimeMs: Long,
    ) {
        // Only words the user has actually heard by the time they paused are eligible - a hard
        // word later in the sentence that hasn't played yet can't be what they were confused by
        // (mirrors SentenceResolver's own "startMs <= effective time" rule for the same reason).
        val heardInSentence = allWords.filter { it.sentenceId == sentenceId && it.startMs <= effectiveTimeMs }

        // Barely anything of this sentence has played - what the user actually heard right before
        // pausing is mostly the tail of the previous sentence, so fold that in too. Otherwise a
        // trigger landing right at a sentence-boundary transcript split only sees the one or two
        // words heard so far in the new sentence, even when the real hard word is the last word of
        // the one before it.
        val eligibleWords = if (heardInSentence.size < AppDefaults.MIN_HEARD_WORDS_BEFORE_SENTENCE_LOOKBACK) {
            val firstWordIndex = allWords.indexOfFirst { it.sentenceId == sentenceId }
            val previousSentenceId = allWords.getOrNull(firstWordIndex - 1)?.sentenceId
            val previousSentenceWords = previousSentenceId?.let { id -> allWords.filter { it.sentenceId == id } }.orEmpty()
            previousSentenceWords + heardInSentence
        } else {
            heardInSentence
        }

        val ranked = WordDifficultyRanker.orderHardestFirst(
            eligibleWords,
            wordDifficultyRepository::rankOf,
            wordDifficultyRepository::isKnownWord,
        )
        if (ranked.isEmpty()) {
            playerController.resume()
            return
        }

        val hardWordCount = ranked.count { wordDifficultyRepository.rankOf(it.word) >= AppDefaults.HARD_WORD_RANK_THRESHOLD }
        if (settingsRepository.autoFullSentenceEnabled.value &&
            hardWordCount >= AppDefaults.AUTO_FULL_SENTENCE_HARD_WORD_COUNT
        ) {
            // Too many hard words for one-at-a-time to be useful - translate the whole sentence
            // instead, same as normal (non-hard-word) mode. Reset the per-sentence progression so
            // a later re-trigger here (e.g. after toggling this setting off) starts clean.
            hardWordSentenceId = null
            _uiState.update { current ->
                if (current !is PlayerScreenState.Ready) return@update current
                current.copy(
                    resolvedSentenceText = sentenceText,
                    translatedSentenceText = null,
                    isTranslating = true,
                    noRelevantSentence = false,
                    activeSentenceId = sentenceId,
                    activeWord = null,
                )
            }
            translateAndSpeak(sentenceText)
            return
        }

        hardWordIndex = if (hardWordSentenceId == sentenceId) (hardWordIndex + 1) % ranked.size else 0
        hardWordSentenceId = sentenceId
        val targetWordEntry = ranked[hardWordIndex]
        val targetWord = targetWordEntry.word.trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
        android.util.Log.d(
            "HARDWORDDEBUG",
            "sentenceId=$sentenceId index=$hardWordIndex/${ranked.size} target=$targetWord " +
                "ranked=${ranked.joinToString { it.word }}",
        )

        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(
                resolvedSentenceText = targetWord,
                translatedSentenceText = null,
                isTranslating = true,
                noRelevantSentence = false,
                // The target word may belong to the previous sentence (see eligibleWords above) -
                // highlight wherever it actually is, not necessarily the trigger's own sentence.
                activeSentenceId = targetWordEntry.sentenceId,
                activeWord = targetWord,
            )
        }
        translateAndSpeak(targetWord, speakEnglishFirst = true)
    }

    private suspend fun translateAndSpeak(englishText: String, speakEnglishFirst: Boolean = false) {
        val result = translationRepository.translateToHebrew(englishText)
        val translated = result.getOrNull()
        var stillRelevant = false
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            // A newer trigger (or a dismiss) may have landed while the translation was in
            // flight; only apply this result if it's still the sentence being shown.
            if (current.resolvedSentenceText != englishText) return@update current
            stillRelevant = true
            current.copy(translatedSentenceText = translated, isTranslating = false)
        }
        // If a dismiss or a newer trigger has already taken over, that flow owns resuming
        // playback - resuming here too would race it or restart audio out from under it.
        if (!stillRelevant) return
        if (speakEnglishFirst) {
            // Hard-word mode: read the source word aloud before its Hebrew translation.
            withTimeoutOrNull(AppDefaults.TTS_WAIT_TIMEOUT_MS) {
                hebrewSpeaker.speak(englishText, HebrewSpeaker.ENGLISH)
            }
        }
        if (translated != null) {
            // Some OEM TTS engines occasionally drop the utterance-done callback; without a
            // timeout a dropped callback would leave the episode paused forever.
            withTimeoutOrNull(AppDefaults.TTS_WAIT_TIMEOUT_MS) { hebrewSpeaker.speak(translated) }
        }
        playerController.resume()
        // Auto-close the translation panel once its narration has actually finished, so it
        // doesn't linger over the transcript after there's nothing left to read - unless a
        // newer trigger (or a manual dismiss) already took over while this one was speaking.
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready || current.resolvedSentenceText != englishText) {
                return@update current
            }
            current.copy(
                resolvedSentenceText = null,
                translatedSentenceText = null,
                isTranslating = false,
                noRelevantSentence = false,
                activeSentenceId = null,
                activeWord = null,
            )
        }
    }

    override fun onCleared() {
        hebrewSpeaker.stop()
    }
}
