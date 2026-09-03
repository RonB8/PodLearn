package com.example.podlingo.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.config.AppDefaults
import com.example.podlingo.core.SentenceResolution
import com.example.podlingo.core.SentenceResolver
import com.example.podlingo.core.WordDifficultyRanker
import com.example.podlingo.core.WordNormalizer
import com.example.podlingo.core.WordTiming
import com.example.podlingo.data.local.entity.EpisodeEntity
import com.example.podlingo.data.local.entity.WordKnowledgeStatus
import com.example.podlingo.data.repository.EpisodeDownloadManager
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.data.repository.PreprocessingProgress
import com.example.podlingo.data.repository.SettingsRepository
import com.example.podlingo.data.repository.TranscriptRepository
import com.example.podlingo.data.repository.TranslationRepository
import com.example.podlingo.data.repository.WordDifficultyRepository
import com.example.podlingo.data.repository.WordKnowledgeRepository
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
    private val episodeDownloadManager: EpisodeDownloadManager,
    private val translationRepository: TranslationRepository,
    private val playerController: PlayerController,
    private val hebrewSpeaker: HebrewSpeaker,
    private val settingsRepository: SettingsRepository,
    private val wordDifficultyRepository: WordDifficultyRepository,
    private val wordKnowledgeRepository: WordKnowledgeRepository,
    private val vocabQuizBuilder: VocabQuizBuilder,
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

    // Vocab calibration: this episode's words grouped by CEFR rank (5=hardest..0=A1), computed
    // once per calibration pass so onCalibrationContinue can cascade to the next tier down.
    private var pendingDifficultyTiers: Map<Int, List<WordTiming>> = emptyMap()

    // Silent in-playback translation popup: this episode's unknown-word occurrences (first
    // occurrence per unique word, sorted by start time) and which of them have already fired this
    // playthrough, so rewinding/replaying doesn't re-pop the same word.
    private var unknownWordOccurrences: List<WordTiming> = emptyList()
    private val firedUnknownWords = mutableSetOf<String>()

    // Read-aloud + hard-word-mode-off reads the whole sentence, not just the triggering word - a
    // sentence with more than one unknown word would otherwise pass this per-word dedup once per
    // word it contains, reading the same sentence aloud again for every one of them.
    private val spokenSentenceIdsForReadAloud = mutableSetOf<String>()

    // End-of-episode quiz: the display-form words awaiting quiz-building once the user says yes.
    private var pendingQuizWords: List<String> = emptyList()

    init {
        viewModelScope.launch { loadEpisode() }

        viewModelScope.launch {
            playerController.playerState.collect { playerUiState ->
                _uiState.update { current ->
                    if (current is PlayerScreenState.Ready) current.copy(player = playerUiState) else current
                }
                checkAutoTranslatePopup(playerUiState.positionMs)
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
                val unknownWords = unknownWordsInEpisode()
                if (unknownWords.isEmpty()) {
                    advanceToNextEpisodeIfEnabled()
                } else {
                    pendingQuizWords = unknownWords
                    _uiState.update { current ->
                        if (current is PlayerScreenState.Ready) current.copy(quizPrompt = true) else current
                    }
                }
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

    /**
     * Resumes playback after a translation trigger's async work (fetch + TTS) finishes - unless
     * the episode ended in the meantime and the end-of-episode quiz flow (prompt or quiz itself)
     * has since taken over, in which case resuming here would play audio behind that dialog.
     */
    private fun resumeIfNotQuizzing() {
        val state = _uiState.value
        if (state is PlayerScreenState.Ready && (state.quizPrompt || state.quiz != null)) return
        playerController.resume()
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

    /**
     * Turning on starts (or resumes) calibrating this episode's hardest words against the user's
     * vocabulary profile - see [startVocabCalibration]. Turning off cancels immediately, closing
     * any calibration panel that happened to be open.
     */
    fun toggleAutoTranslate() {
        val turningOn = !settingsRepository.autoTranslateEnabled.value
        settingsRepository.setAutoTranslateEnabled(turningOn)
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(autoTranslateEnabled = turningOn, vocabCalibration = if (turningOn) current.vocabCalibration else null)
        }
        if (turningOn) {
            viewModelScope.launch { startVocabCalibration() }
        } else {
            unknownWordOccurrences = emptyList()
        }
    }

    fun onCalibrationWordToggled(word: String) {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val calibration = current.vocabCalibration ?: return@update current
            val selected = if (word in calibration.selected) calibration.selected - word else calibration.selected + word
            current.copy(vocabCalibration = calibration.copy(selected = selected))
        }
    }

    /** Toggles between selecting every word in this tier and clearing the selection - handy when most of the tier is unfamiliar, so the user can select all and then tap off the few they do know. */
    fun onCalibrationSelectAllToggled() {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val calibration = current.vocabCalibration ?: return@update current
            val allSelected = calibration.selected.size == calibration.words.size
            val selected = if (allSelected) emptySet() else calibration.words.toSet()
            current.copy(vocabCalibration = calibration.copy(selected = selected))
        }
    }

    fun onCalibrationContinue() {
        val calibration = (_uiState.value as? PlayerScreenState.Ready)?.vocabCalibration ?: return
        viewModelScope.launch {
            for (word in calibration.words) {
                if (word in calibration.selected) wordKnowledgeRepository.markUnknown(word) else wordKnowledgeRepository.markKnown(word)
            }
            val selectionRate = calibration.selected.size.toDouble() / calibration.words.size
            val nextTierRank = pendingDifficultyTiers.keys.filter { it < calibration.tierRank }.maxOrNull()
            if (selectionRate >= AppDefaults.CALIBRATION_CASCADE_THRESHOLD && nextTierRank != null) {
                val nextTierWords = undecidedWordsInTier(nextTierRank)
                if (nextTierWords.isNotEmpty()) {
                    openCalibrationTier(nextTierRank, nextTierWords)
                    return@launch
                }
            }
            finishCalibration()
        }
    }

    fun dismissTranslationPopup() {
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) current.copy(translationPopup = null) else current
        }
    }

    fun onQuizPromptAnswer(startQuiz: Boolean) {
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) current.copy(quizPrompt = false) else current
        }
        if (!startQuiz) {
            advanceToNextEpisodeIfEnabled()
            return
        }
        viewModelScope.launch {
            val questions = vocabQuizBuilder.buildQuizQuestions(pendingQuizWords)
            if (questions.isEmpty()) {
                advanceToNextEpisodeIfEnabled()
                return@launch
            }
            _uiState.update { current ->
                if (current is PlayerScreenState.Ready) current.copy(quiz = VocabQuizState(questions = questions)) else current
            }
        }
    }

    fun onQuizAnswerSelected(answer: String) {
        val quiz = (_uiState.value as? PlayerScreenState.Ready)?.quiz ?: return
        if (quiz.answeredThisQuestion != null) return
        val question = quiz.questions.getOrNull(quiz.currentIndex) ?: return
        val isCorrect = answer == question.correctAnswer
        viewModelScope.launch {
            if (isCorrect) wordKnowledgeRepository.markKnown(question.word)
            _uiState.update { current ->
                if (current !is PlayerScreenState.Ready) return@update current
                val latestQuiz = current.quiz ?: return@update current
                current.copy(
                    quiz = latestQuiz.copy(
                        answeredThisQuestion = answer,
                        correctCount = latestQuiz.correctCount + if (isCorrect) 1 else 0,
                    ),
                )
            }
        }
    }

    fun onQuizNext() {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val quiz = current.quiz ?: return@update current
            val nextIndex = quiz.currentIndex + 1
            val advanced = if (nextIndex >= quiz.questions.size) {
                quiz.copy(finished = true)
            } else {
                quiz.copy(currentIndex = nextIndex, answeredThisQuestion = null)
            }
            current.copy(quiz = advanced)
        }
    }

    fun onQuizDismissed() {
        _uiState.update { current -> if (current is PlayerScreenState.Ready) current.copy(quiz = null) else current }
        advanceToNextEpisodeIfEnabled()
    }

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun dismissSentenceOverlay() {
        hebrewSpeaker.stop()
        resumeIfNotQuizzing()
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

    private suspend fun loadEpisode() {
        val episode = podcastRepository.getEpisode(episodeId)
        if (episode == null) {
            _uiState.value = PlayerScreenState.Failed("Episode not found")
            return
        }
        observeDownload(episode)
    }

    /** The Failed screen's retry action - re-runs the same load path a fresh episode open would take. */
    fun retry() {
        viewModelScope.launch { loadEpisode() }
    }

    // Routed through EpisodeDownloadManager rather than calling transcriptRepository.preprocess()
    // directly - that keeps a download running (and shared with any other observer) even if this
    // ViewModel is cleared because the user left the Player screen. Every episode goes through
    // here, READY ones included: preprocess() itself is what re-downloads audio that storage
    // eviction cleared while the transcript stayed READY, so skipping it here would leave a
    // READY-but-evicted episode unable to ever play again.
    private suspend fun observeDownload(episode: EpisodeEntity) {
        episodeDownloadManager.progressFor(episode).collect { progress ->
            when (progress) {
                null -> Unit
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
            autoTranslateEnabled = settingsRepository.autoTranslateEnabled.value,
        )
        podcastRepository.recordEpisodePlayed(episode.id)
        // A fresh episode brings its own hardest words - if auto-translate is already on and this
        // episode hasn't been calibrated before, offer it once. Without the vocabCalibrated guard
        // this would re-prompt every time the player screen is recreated for the same episode
        // (e.g. swipe-down-dismiss then reopening it), even after the user already went through
        // calibration and is just listening - re-running this on the same episode is now only
        // available via an explicit "Auto translate" chip tap (see toggleAutoTranslate).
        if (settingsRepository.autoTranslateEnabled.value) {
            if (!episode.vocabCalibrated) {
                startVocabCalibration()
            } else {
                // Already calibrated in an earlier session - unknownWordOccurrences is per-instance
                // state (this is a fresh ViewModel), so it needs repopulating here or the popup/
                // read-aloud would silently never fire again on this episode.
                refreshUnknownWordOccurrences()
            }
        }
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
                        resumeIfNotQuizzing()
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
                    resumeIfNotQuizzing()
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
            resumeIfNotQuizzing()
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

    private suspend fun translateAndSpeak(
        englishText: String,
        speakEnglishFirst: Boolean = false,
        translate: suspend () -> String? = { translationRepository.translateToHebrew(englishText).getOrNull() },
    ) {
        val translated = translate()
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
        resumeIfNotQuizzing()
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

    // --- Vocabulary trainer: calibration ---------------------------------------------------

    /**
     * Groups this episode's unique words by Oxford CEFR rank (5 = unranked/hardest .. 0 = A1),
     * reusing the same proper-noun/punctuation filtering as hard-word mode - just applied to the
     * whole episode instead of one sentence at a time.
     */
    private fun computeDifficultyTiers(words: List<WordTiming>): Map<Int, List<WordTiming>> {
        val deduped = words.distinctBy { WordNormalizer.normalize(it.word) }
        val ranked = WordDifficultyRanker.orderHardestFirst(
            deduped,
            wordDifficultyRepository::rankOf,
            wordDifficultyRepository::isKnownWord,
        )
        return ranked.groupBy { wordDifficultyRepository.rankOf(it.word) }
    }

    /** Opens the calibration panel on the hardest tier with anything left to ask about, or just refreshes popup tracking if every tier is already decided. */
    private suspend fun startVocabCalibration() {
        // Offering it now (whether a panel actually opens or there's nothing new to ask) is what
        // startPlayback's auto-trigger checks - it should only ever happen once per episode
        // automatically; a later manual "Auto translate" chip tap can still re-invoke this.
        podcastRepository.markEpisodeVocabCalibrated(episodeId)
        val words = cachedWords ?: transcriptRepository.getWordTimings(episodeId).also { cachedWords = it }
        pendingDifficultyTiers = computeDifficultyTiers(words)
        for (rank in pendingDifficultyTiers.keys.sortedDescending()) {
            val undecided = undecidedWordsInTier(rank)
            if (undecided.isNotEmpty()) {
                openCalibrationTier(rank, undecided)
                return
            }
        }
        refreshUnknownWordOccurrences()
    }

    /** This tier's words minus any that already have a known/unknown row - never re-ask about a decided word. */
    private suspend fun undecidedWordsInTier(rank: Int): List<String> {
        val tierWords = pendingDifficultyTiers[rank].orEmpty()
        if (tierWords.isEmpty()) return emptyList()
        val statuses = wordKnowledgeRepository.getStatuses(tierWords.map { it.word })
        return tierWords.filter { WordNormalizer.normalize(it.word) !in statuses }.map { it.word }
    }

    private fun openCalibrationTier(rank: Int, words: List<String>) {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(vocabCalibration = VocabCalibrationState(tierRank = rank, words = words))
        }
    }

    private suspend fun finishCalibration() {
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) current.copy(vocabCalibration = null) else current
        }
        refreshUnknownWordOccurrences()
    }

    // --- Vocabulary trainer: silent in-playback popup ---------------------------------------

    private suspend fun refreshUnknownWordOccurrences() {
        val words = cachedWords ?: transcriptRepository.getWordTimings(episodeId).also { cachedWords = it }
        val deduped = words.distinctBy { WordNormalizer.normalize(it.word) }
        val statuses = wordKnowledgeRepository.getStatuses(deduped.map { it.word })
        unknownWordOccurrences = deduped
            .filter { statuses[WordNormalizer.normalize(it.word)]?.status == WordKnowledgeStatus.UNKNOWN }
            .sortedBy { it.startMs }
    }

    /**
     * Called on every position update while playing - fires at most once per word per playthrough.
     * Scoped to the sentence currently at [positionMs] only - after backgrounding/seeking far
     * ahead, this deliberately does NOT "catch up" on unknown words from sentences already passed;
     * it only ever surfaces words from the sentence actually being heard right now. If the user
     * later scrubs back into an earlier sentence, its words become eligible again naturally.
     *
     * Both checks use [positionMs] minus [AppDefaults.REACTION_DELAY_MS] as the effective time,
     * not the raw position - firing right at a word's startMs pauses/reads it before it's finished
     * being spoken. Deriving the current sentence from that same delayed reference (rather than the
     * raw position) keeps a word eligible under its own sentence even when it falls right at a
     * sentence boundary, instead of the delay alone pushing "current sentence" into the next one.
     */
    private fun checkAutoTranslatePopup(positionMs: Long) {
        val current = _uiState.value
        if (current !is PlayerScreenState.Ready || !current.autoTranslateEnabled) return
        val effectiveTimeMs = positionMs - AppDefaults.REACTION_DELAY_MS
        val currentSentenceId = current.sentences.lastOrNull { it.startMs <= effectiveTimeMs }?.id ?: return
        val readAloud = settingsRepository.autoTranslateReadAloudEnabled.value
        val sentenceMode = readAloud && !settingsRepository.hardWordModeEnabled.value
        // Sentence mode reads the whole sentence for whichever unknown word triggers it first - a
        // second (or third) unknown word later in that same sentence must not re-trigger it.
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
                _uiState.update { latest ->
                    if (latest !is PlayerScreenState.Ready) return@update latest
                    latest.copy(translationPopup = WordTranslationPopup(next.word, translation))
                }
            }
        }
    }

    /**
     * Read-aloud variant of the auto-translate popup: pauses playback and speaks through the same
     * overlay/TTS path the manual trigger uses, instead of the silent banner. Hard-word mode
     * restricts this to just the unknown word (English then Hebrew, using the word-knowledge
     * cache so a word already looked up never costs a second OpenAI call); otherwise the whole
     * sentence it appears in is read (Hebrew only, same as the manual trigger's own sentence
     * mode - sentence translations aren't cached, matching existing trigger behavior).
     */
    private suspend fun speakAutoTranslatedWord(occurrence: WordTiming) {
        playerController.pause()
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) current.copy(transcriptVisible = true) else current
        }
        if (settingsRepository.hardWordModeEnabled.value) {
            val word = occurrence.word.trim { !it.isLetterOrDigit() && it != '\'' && it != '-' }
            _uiState.update { current ->
                if (current !is PlayerScreenState.Ready) return@update current
                current.copy(
                    resolvedSentenceText = word,
                    translatedSentenceText = null,
                    isTranslating = true,
                    noRelevantSentence = false,
                    activeSentenceId = occurrence.sentenceId,
                    activeWord = word,
                )
            }
            translateAndSpeak(word, speakEnglishFirst = true) { wordKnowledgeRepository.getOrFetchTranslation(word) }
        } else {
            val sentence = transcriptRepository.getSentence(occurrence.sentenceId)
            if (sentence == null) {
                resumeIfNotQuizzing()
                return
            }
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

    // --- Vocabulary trainer: end-of-episode quiz ---------------------------------------------

    private suspend fun unknownWordsInEpisode(): List<String> = vocabQuizBuilder.unknownWordsInEpisode(episodeId)

    private fun advanceToNextEpisodeIfEnabled() {
        val next = nextEpisodeId ?: return
        if (settingsRepository.autoPlayNextEnabled.value) _navigateToEpisode.tryEmit(next)
    }

    override fun onCleared() {
        hebrewSpeaker.stop()
    }
}
