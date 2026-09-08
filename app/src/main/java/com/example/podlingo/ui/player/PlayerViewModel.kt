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
import com.example.podlingo.data.local.entity.SentenceEntity
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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

    // The job currently fetching+narrating a trigger's translation, so a retrigger arriving
    // mid-flight can cancel it outright instead of letting its eventual resumeIfNotQuizzing()
    // race the new one and resume real playback out from under it.
    private var activeTriggerJob: Job? = null

    // Full-sentence mode retrigger backtracking. anchorFullSentenceId is the sentence
    // SentenceResolver actually resolved from the *last* trigger's real pause position; when a
    // new trigger resolves to that same anchor, no new audio has actually played since, so the
    // user is retriggering because they want to hear something earlier rather than the same
    // sentence again - see handleTrigger and showPreviousFullSentence. lastShownFullSentenceId is
    // whatever sentence's translation is currently (or was most recently) displayed, which is what
    // backtracking steps back from - it may already be earlier than the anchor after one or more
    // backward steps.
    private var anchorFullSentenceId: String? = null
    private var lastShownFullSentenceId: String? = null

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

    // Pre-episode Word Check: true once anything has been explicitly skipped (Quiz tab) during the
    // current Word Check session - see finishWordCheck. Reset at the start of each session so a
    // fully-resolved pass (no skips) is the only kind that ever marks the episode complete.
    private var wordCheckHasUnresolved = false

    // The tier currently being built (translations fetched, questions assembled) - cancelled
    // before starting a new one so a stray duplicate build (see beginWordCheck) can never race the
    // one actually meant to show.
    private var wordCheckBuildJob: Job? = null

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
            playerController.previousSentenceRequests.collect {
                showPreviousFullSentence()
            }
        }

        viewModelScope.launch {
            uiState.collect { state ->
                val overlayActive = state is PlayerScreenState.Ready &&
                    !state.hardWordModeTriggerEnabled &&
                    (state.isTranslating || state.resolvedSentenceText != null)
                playerController.setTranslationOverlayActive(overlayActive)
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
            settingsRepository.hardWordModeTriggerEnabled.collect { enabled ->
                _uiState.update { current ->
                    if (current is PlayerScreenState.Ready) current.copy(hardWordModeTriggerEnabled = enabled) else current
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.showSentenceTranslationsEnabled.collect { enabled ->
                _uiState.update { current ->
                    if (current is PlayerScreenState.Ready) current.copy(showSentenceTranslationsEnabled = enabled) else current
                }
            }
        }

    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state !is PlayerScreenState.Ready) return
        // Deciding whether a play attempt while paused actually means "resume" or "show me the
        // previous sentence" (a full-sentence trigger still in progress, or one that just finished)
        // happens centrally in PlaybackService - see translationOverlayActive - so this behaves the
        // same regardless of whether play came from this in-app button, headphones, the
        // notification, or the lock screen.
        if (state.player.isPlaying) playerController.pause() else playerController.play()
    }

    /**
     * Resumes playback after a translation trigger's async work (fetch + TTS) finishes - unless
     * the episode ended in the meantime and the end-of-episode quiz flow (prompt or quiz itself)
     * has since taken over, in which case resuming here would play audio behind that dialog.
     */
    private fun resumeIfNotQuizzing() {
        val state = _uiState.value
        if (state is PlayerScreenState.Ready && (state.quizPrompt || state.quiz != null || state.wordCheck != null)) return
        // Must happen synchronously, right here, rather than left to the uiState collector in
        // init that normally drives this flag - that collector reacts on its own coroutine, so it
        // can still be running behind the state change (resolvedSentenceText/isTranslating already
        // cleared) that led to this resume() call. A resume reaching PlaybackService while this
        // flag is still stale-true gets misread as "show the previous sentence" instead of an
        // actual resume (see TriggerEventBus.translationOverlayActive), which is exactly what
        // happened when auto-translate's own end-of-narration resume raced that collector.
        playerController.setTranslationOverlayActive(false)
        playerController.resume()
    }

    fun skipToNextEpisode() {
        nextEpisodeId?.let { _navigateToEpisode.tryEmit(it) }
    }

    fun skipForward() {
        resetFullSentenceBacktracking()
        playerController.seekForward()
    }

    fun skipBackward() {
        resetFullSentenceBacktracking()
        playerController.seekBackward()
    }

    /**
     * A manual seek/skip means the next trigger's pause position has nothing to do with whatever
     * was last shown - without this, a coincidental match against a stale [anchorFullSentenceId]
     * would incorrectly treat the next trigger as a "nothing new was heard" retrigger.
     */
    private fun resetFullSentenceBacktracking() {
        anchorFullSentenceId = null
        lastShownFullSentenceId = null
    }

    fun setPlaybackSpeed(speed: Float) = playerController.setPlaybackSpeed(speed)

    fun toggleTranscript() {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(transcriptVisible = !current.transcriptVisible)
        }
    }

    fun toggleShowSentenceTranslations() {
        settingsRepository.setShowSentenceTranslationsEnabled(!settingsRepository.showSentenceTranslationsEnabled.value)
    }

    /**
     * Lazily fetches a sentence's Hebrew translation for the always-on under-each-sentence display,
     * called as each sentence scrolls into view - not the trigger overlay's on-demand translation.
     * Like that overlay, results aren't persisted: [PlayerScreenState.Ready.sentenceTranslations]
     * only lives for this playthrough's ViewModel instance.
     */
    fun ensureSentenceTranslation(sentence: SentenceEntity) {
        val current = _uiState.value
        if (current !is PlayerScreenState.Ready) return
        if (sentence.id in current.sentenceTranslations || sentence.id in current.translatingSentenceIds) return
        _uiState.update { latest ->
            if (latest !is PlayerScreenState.Ready) return@update latest
            latest.copy(translatingSentenceIds = latest.translatingSentenceIds + sentence.id)
        }
        viewModelScope.launch {
            val translation = translationRepository.translateToHebrew(sentence.fullText).getOrNull()
            _uiState.update { latest ->
                if (latest !is PlayerScreenState.Ready) return@update latest
                latest.copy(
                    translatingSentenceIds = latest.translatingSentenceIds - sentence.id,
                    sentenceTranslations = if (translation != null) {
                        latest.sentenceTranslations + (sentence.id to translation)
                    } else {
                        latest.sentenceTranslations
                    },
                )
            }
        }
    }

    /**
     * Turning on opens (or resumes) the pre-episode "Word Check" assessment for this episode if
     * it hasn't been fully resolved yet - see [maybeOpenWordCheck]. Turning off cancels
     * immediately, closing any Word Check dialog that happened to be open.
     */
    fun toggleAutoTranslate() {
        val turningOn = !settingsRepository.autoTranslateEnabled.value
        settingsRepository.setAutoTranslateEnabled(turningOn)
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(autoTranslateEnabled = turningOn, wordCheck = if (turningOn) current.wordCheck else null)
        }
        if (turningOn) {
            viewModelScope.launch { maybeOpenWordCheck() }
        } else {
            unknownWordOccurrences = emptyList()
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

    /** Shared by the manual "Next" tap and "Skip" (see VocabQuizDialog) - Skip just calls this before an answer was ever recorded. */
    fun onQuizNext() {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val q = current.quiz ?: return@update current
            val nextIndex = q.currentIndex + 1
            current.copy(
                quiz = if (nextIndex >= q.questions.size) {
                    q.copy(finished = true)
                } else {
                    q.copy(currentIndex = nextIndex, answeredThisQuestion = null)
                },
            )
        }
    }

    fun onQuizDismissed() {
        _uiState.update { current -> if (current is PlayerScreenState.Ready) current.copy(quiz = null) else current }
        advanceToNextEpisodeIfEnabled()
    }

    fun seekTo(positionMs: Long) {
        resetFullSentenceBacktracking()
        playerController.seekTo(positionMs)
    }

    fun dismissSentenceOverlay() {
        activeTriggerJob?.cancel()
        resetFullSentenceBacktracking()
        hebrewSpeaker.stop()
        // Must clear resolvedSentenceText/isTranslating *before* resumeIfNotQuizzing() below -
        // PlaybackService treats any resume attempt while they're still set as "show the previous
        // sentence" (see translationOverlayActive), so resuming first would immediately walk back
        // a sentence right after a plain dismiss.
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
        resumeIfNotQuizzing()
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
        // Must be checked before prepare() - prepare() itself no-ops (and leaves whatever was
        // already playing alone) when this episode is already the loaded one, which is exactly
        // the "not a fresh start" case Word Check should never re-offer mid-playthrough.
        val isFreshStart = playerController.playerState.value.episodeId != episode.id
        val words = transcriptRepository.getWordTimings(episode.id).also { cachedWords = it }
        val autoTranslateOn = settingsRepository.autoTranslateEnabled.value
        val wordCheckTiers = if (isFreshStart && autoTranslateOn && !episode.startQuizCompleted) {
            computeOutstandingWordTiers(words)
        } else {
            emptyList()
        }
        val openWordCheck = wordCheckTiers.isNotEmpty()

        val artworkUrl = podcastRepository.getPodcast(episode.podcastId)?.imageUrl
        // autoPlay=false when Word Check is about to open - otherwise the episode would audibly
        // start for a moment before the dialog catches up to it.
        playerController.prepare(episode.id, episode.title, artworkUrl, localFilePath, autoPlay = !openWordCheck)
        val podcastEpisodes = podcastRepository.getEpisodes(episode.podcastId).first()
        val currentIndex = podcastEpisodes.indexOfFirst { it.id == episode.id }
        nextEpisodeId = if (currentIndex == -1) null else podcastEpisodes.getOrNull(currentIndex + 1)?.id
        val sentences = transcriptRepository.getSentences(episode.id)
        _uiState.value = PlayerScreenState.Ready(
            episodeId = episode.id,
            episodeTitle = episode.title,
            player = playerController.playerState.value,
            artworkUrl = artworkUrl,
            hasNextEpisode = nextEpisodeId != null,
            sentences = sentences,
            words = words,
            hardWordModeTriggerEnabled = settingsRepository.hardWordModeTriggerEnabled.value,
            autoTranslateEnabled = autoTranslateOn,
            showSentenceTranslationsEnabled = settingsRepository.showSentenceTranslationsEnabled.value,
            wordCheckLoading = openWordCheck,
        )
        podcastRepository.recordEpisodePlayed(episode.id)
        if (openWordCheck) {
            beginWordCheck(wordCheckTiers)
        } else if (autoTranslateOn) {
            // Nothing left to assess (or not a fresh start) - unknownWordOccurrences is per-instance
            // state (this is a fresh ViewModel), so it needs repopulating here or the popup/
            // read-aloud would silently never fire again on this episode.
            refreshUnknownWordOccurrences()
        }
    }

    private fun handleTrigger(pauseTimeMs: Long) {
        // A trigger firing means there's a translation to show - surface it in the transcript
        // (covering the upcoming sentences) rather than relying on the user having it open already.
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) current.copy(transcriptVisible = true) else current
        }
        activeTriggerJob?.cancel()
        activeTriggerJob = viewModelScope.launch {
            val words = cachedWords ?: transcriptRepository.getWordTimings(episodeId).also { cachedWords = it }
            val effectiveTimeMs = pauseTimeMs - AppDefaults.REACTION_DELAY_MS
            when (
                val resolution = SentenceResolver.resolve(
                    pauseTimeMs,
                    AppDefaults.REACTION_DELAY_MS,
                    words,
                    sentenceStartGraceMs = AppDefaults.SENTENCE_START_GRACE_MS,
                )
            ) {
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
                    if (settingsRepository.hardWordModeTriggerEnabled.value) {
                        handleHardWordTrigger(resolution.sentenceId, sentence.fullText, words, effectiveTimeMs)
                    } else {
                        // Leaving hard-word mode's per-sentence progression - restart clean if
                        // it's ever re-entered on this sentence.
                        hardWordSentenceId = null
                        // Resolving to the same sentence as the last trigger means no real playback
                        // has happened since - the user isn't asking to hear that sentence again,
                        // they're asking for something earlier (see anchorFullSentenceId).
                        val sameAnchorAsLastTrigger = resolution.sentenceId == anchorFullSentenceId
                        anchorFullSentenceId = resolution.sentenceId
                        val target = if (sameAnchorAsLastTrigger) {
                            previousFullSentence(lastShownFullSentenceId) ?: sentence
                        } else {
                            sentence
                        }
                        showFullSentenceTranslation(target)
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
     * Full-sentence mode only (see [togglePlayPause]): retriggering while a sentence's translation
     * is still being read, or immediately after, means "I wanted the sentence before that one" -
     * cancels whatever is currently being fetched/narrated and shows [lastShownFullSentenceId]'s
     * predecessor instead. Each further retrigger before any real playback happens keeps walking
     * back one more sentence, since it updates [lastShownFullSentenceId] the same way a fresh
     * trigger would.
     */
    private fun showPreviousFullSentence() {
        val target = previousFullSentence(lastShownFullSentenceId) ?: return
        activeTriggerJob?.cancel()
        hebrewSpeaker.stop()
        activeTriggerJob = viewModelScope.launch { showFullSentenceTranslation(target) }
    }

    private fun previousFullSentence(sentenceId: String?): SentenceEntity? {
        val state = _uiState.value as? PlayerScreenState.Ready ?: return null
        val index = state.sentences.indexOfFirst { it.id == sentenceId }
        if (index <= 0) return null
        return state.sentences[index - 1]
    }

    private suspend fun showFullSentenceTranslation(sentence: SentenceEntity) {
        lastShownFullSentenceId = sentence.id
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
        translateAndSpeak(sentence.fullText, speakEnglishFirst = true) {
            translateSentenceCached(sentence.id, sentence.fullText)
        }
    }

    /**
     * Reuses the always-on under-transcript translation ([PlayerScreenState.Ready.sentenceTranslations],
     * filled in by [ensureSentenceTranslation]) instead of re-fetching it for the trigger overlay
     * when it's already sitting there - and vice versa, caches a fresh fetch here so a later scroll
     * into view doesn't re-fetch it either.
     */
    private suspend fun translateSentenceCached(sentenceId: String, sentenceText: String): String? {
        val cached = (_uiState.value as? PlayerScreenState.Ready)?.sentenceTranslations?.get(sentenceId)
        if (cached != null) return cached
        val translated = translationRepository.translateToHebrew(sentenceText).getOrNull()
        if (translated != null) {
            _uiState.update { current ->
                if (current !is PlayerScreenState.Ready) return@update current
                current.copy(sentenceTranslations = current.sentenceTranslations + (sentenceId to translated))
            }
        }
        return translated
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
            translateAndSpeak(sentenceText, speakEnglishFirst = true) {
                translateSentenceCached(sentenceId, sentenceText)
            }
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
        // Auto-close the translation panel once its narration has actually finished, so it
        // doesn't linger over the transcript after there's nothing left to read - unless a
        // newer trigger (or a manual dismiss) already took over while this one was speaking.
        // This must happen *before* resumeIfNotQuizzing() below: PlaybackService treats any
        // resume attempt while resolvedSentenceText is still set as "show the previous sentence"
        // (see translationOverlayActive) - resuming first would immediately walk back a sentence
        // with no actual retrigger from the user.
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
        resumeIfNotQuizzing()
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

    // --- Vocabulary trainer: pre-episode Word Check -----------------------------------------

    /**
     * This episode's still-outstanding words - anything without a KNOWN row, whether previously
     * flagged UNKNOWN or never decided at all - grouped hardest-tier-first the same way as
     * [computeDifficultyTiers]. Empty tiers are dropped, so an empty overall result means nothing
     * left to assess.
     */
    private suspend fun computeOutstandingWordTiers(words: List<WordTiming>): List<List<String>> {
        val deduped = words.distinctBy { WordNormalizer.normalize(it.word) }
        val statuses = wordKnowledgeRepository.getStatuses(deduped.map { it.word })
        val outstanding = deduped.filter { statuses[WordNormalizer.normalize(it.word)]?.status != WordKnowledgeStatus.KNOWN }
        if (outstanding.isEmpty()) return emptyList()
        val tiers = computeDifficultyTiers(outstanding)
        return tiers.keys.sortedDescending().map { rank -> tiers.getValue(rank).map { it.word } }
    }

    /** Opens Word Check on this episode's hardest outstanding tier if there's anything left to assess, or just refreshes popup tracking otherwise - shared by [startPlayback]'s fresh-start check and a manual "Auto translate" chip tap. */
    private suspend fun maybeOpenWordCheck() {
        if ((_uiState.value as? PlayerScreenState.Ready)?.wordCheck != null) return
        val episode = podcastRepository.getEpisode(episodeId)
        if (episode?.startQuizCompleted == true) {
            refreshUnknownWordOccurrences()
            return
        }
        val words = cachedWords ?: transcriptRepository.getWordTimings(episodeId).also { cachedWords = it }
        val tiers = computeOutstandingWordTiers(words)
        if (tiers.isEmpty()) refreshUnknownWordOccurrences() else beginWordCheck(tiers)
    }

    /**
     * Starts a fresh Word Check session on [tiers] - resets the per-session "was anything left
     * undecided" tracking used by [finishWordCheck] to decide whether this episode counts as fully
     * done. Cancels any build already in flight first: [startPlayback] can in principle run more
     * than once for the same fresh load (the download-progress flow re-emitting), and without this
     * a second build would silently race the first one instead of superseding it.
     */
    private fun beginWordCheck(tiers: List<List<String>>) {
        wordCheckHasUnresolved = false
        wordCheckBuildJob?.cancel()
        wordCheckBuildJob = viewModelScope.launch { openWordCheckTier(tiers) }
    }

    /**
     * Builds the given tiers' hardest one's quiz questions and opens Word Check on it - defaults
     * to the Simple tab when the tier has no buildable question (e.g. not enough real distractors
     * exist yet), so there's always something the user can act on. Sets [WordCheckState] loading
     * while this runs (see [PlayerScreenState.Ready.wordCheckLoading]) - a tier full of never-seen
     * words needs a live translation lookup per word (batched, but still real network time), and
     * without a loading state the screen would otherwise look frozen: no audio playing yet, no
     * dialog on screen either.
     */
    private suspend fun openWordCheckTier(tiers: List<List<String>>) {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(wordCheckLoading = true)
        }
        val tierWords = tiers.first()
        val remaining = tiers.drop(1)
        val questions = vocabQuizBuilder.buildQuizQuestions(tierWords)
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            current.copy(
                wordCheckLoading = false,
                wordCheck = WordCheckState(
                    tab = if (questions.isNotEmpty()) WordCheckTab.QUIZ else WordCheckTab.SIMPLE,
                    words = tierWords,
                    questions = questions,
                    remainingTiers = remaining,
                ),
            )
        }
    }

    fun onWordCheckTabSelected(tab: WordCheckTab) {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val wc = current.wordCheck ?: return@update current
            current.copy(wordCheck = wc.copy(tab = tab))
        }
    }

    fun onWordCheckAnswerSelected(answer: String) {
        val wc = (_uiState.value as? PlayerScreenState.Ready)?.wordCheck ?: return
        if (wc.answeredThisQuestion != null) return
        val question = wc.questions.getOrNull(wc.currentIndex) ?: return
        val isCorrect = answer == question.correctAnswer
        viewModelScope.launch {
            if (isCorrect) wordKnowledgeRepository.markKnown(question.word) else wordKnowledgeRepository.markUnknown(question.word)
            _uiState.update { current ->
                if (current !is PlayerScreenState.Ready) return@update current
                val latest = current.wordCheck ?: return@update current
                current.copy(
                    wordCheck = latest.copy(
                        answeredThisQuestion = answer,
                        quizCorrectWords = if (isCorrect) latest.quizCorrectWords + question.word else latest.quizCorrectWords,
                        // Pre-select it in the Simple tab, same as if the user had tapped it there -
                        // still freely editable from that tab afterwards.
                        tapSelected = if (isCorrect) latest.tapSelected else latest.tapSelected + question.word,
                    ),
                )
            }
            delay(AppDefaults.START_QUIZ_AUTO_ADVANCE_DELAY_MS)
            advanceWordCheckQuestion()
        }
    }

    /** Leaves the word's status untouched - see [WordCheckState.skipped] - and moves on, same as a normal advance. */
    fun onWordCheckSkip() {
        val wc = (_uiState.value as? PlayerScreenState.Ready)?.wordCheck ?: return
        if (wc.answeredThisQuestion != null) return
        val question = wc.questions.getOrNull(wc.currentIndex) ?: return
        wordCheckHasUnresolved = true
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val latest = current.wordCheck ?: return@update current
            current.copy(wordCheck = latest.copy(skipped = latest.skipped + question.word))
        }
        advanceWordCheckQuestion()
    }

    /** Steps to the next question, or - once the tier's questions run out - switches to the Simple tab so the user finalizes the tier (and any words that never got a question) with Continue. */
    private fun advanceWordCheckQuestion() {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val wc = current.wordCheck ?: return@update current
            val nextIndex = wc.currentIndex + 1
            current.copy(
                wordCheck = if (nextIndex < wc.questions.size) {
                    wc.copy(currentIndex = nextIndex, answeredThisQuestion = null)
                } else {
                    wc.copy(tab = WordCheckTab.SIMPLE)
                },
            )
        }
    }

    /** Toggles the Simple tab's "I don't know this" selection - a no-op for a word already resolved correctly via the Quiz tab. Clears [WordCheckState.skipped] for it, so tapping a skipped word always brings it back into the normal decide-via-Continue pool. */
    fun onWordCheckWordToggled(word: String) {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val wc = current.wordCheck ?: return@update current
            if (word in wc.quizCorrectWords) return@update current
            val selected = if (word in wc.tapSelected) wc.tapSelected - word else wc.tapSelected + word
            current.copy(wordCheck = wc.copy(tapSelected = selected, skipped = wc.skipped - word))
        }
    }

    /** Toggles between selecting every still-undecided word in this tier and clearing the selection - handy when most of the tier is unfamiliar. Also clears [WordCheckState.skipped], same bulk-override reasoning. */
    fun onWordCheckSelectAllToggled() {
        _uiState.update { current ->
            if (current !is PlayerScreenState.Ready) return@update current
            val wc = current.wordCheck ?: return@update current
            val toggleable = (wc.words - wc.quizCorrectWords).toSet()
            val allSelected = toggleable.isNotEmpty() && wc.tapSelected.containsAll(toggleable)
            current.copy(wordCheck = wc.copy(tapSelected = if (allSelected) emptySet() else toggleable, skipped = emptySet()))
        }
    }

    /**
     * Finalizes this tier: every word gets a known/unknown status except ones explicitly skipped
     * in the Quiz tab (those are left alone so they resurface on a future Word Check instead of
     * silently ending up "known" - see [WordCheckState.skipped]). Marking at least
     * [AppDefaults.CALIBRATION_CASCADE_THRESHOLD] of this tier's decided words "don't know" pulls
     * in the next-easier tier, same as calibration always did - otherwise Word Check finishes here
     * even though easier tiers may still have outstanding words (they simply won't come up again
     * until something else - the in-playback popup, or another Word Check pass reaching them via
     * a worse showing on a harder tier - touches them).
     */
    fun onWordCheckContinue() {
        val wc = (_uiState.value as? PlayerScreenState.Ready)?.wordCheck ?: return
        viewModelScope.launch {
            for (word in wc.words) {
                if (word in wc.quizCorrectWords || word in wc.skipped) continue
                if (word in wc.tapSelected) wordKnowledgeRepository.markUnknown(word) else wordKnowledgeRepository.markKnown(word)
            }
            if (wc.skipped.isNotEmpty()) wordCheckHasUnresolved = true
            val decidedCount = wc.words.size - wc.skipped.size
            val notKnownRate = if (decidedCount > 0) wc.tapSelected.size.toDouble() / decidedCount else 0.0
            if (notKnownRate >= AppDefaults.CALIBRATION_CASCADE_THRESHOLD && wc.remainingTiers.isNotEmpty()) {
                openWordCheckTier(wc.remainingTiers)
            } else {
                finishWordCheck()
            }
        }
    }

    /** Only a pass with nothing left unresolved (no skips, anywhere across the session) counts as "done" - marking the episode complete on a partial pass would silently drop the rest instead of offering it again next time. */
    private suspend fun finishWordCheck() {
        if (!wordCheckHasUnresolved) podcastRepository.markStartQuizCompleted(episodeId)
        _uiState.update { current -> if (current is PlayerScreenState.Ready) current.copy(wordCheck = null) else current }
        refreshUnknownWordOccurrences()
        playerController.play()
    }

    /** Closes Word Check without deciding anything left in the current tier - the episode is never marked complete this way, so the same tier (minus whatever was already answered/committed) is offered again on the next fresh start. */
    fun onWordCheckDismissed() {
        // Also covers dismissing the loading state before a tier finishes building - without
        // cancelling the job here, it would complete moments later and reopen the dialog right
        // out from under a user who just closed it.
        wordCheckBuildJob?.cancel()
        _uiState.update { current ->
            if (current is PlayerScreenState.Ready) current.copy(wordCheck = null, wordCheckLoading = false) else current
        }
        viewModelScope.launch { refreshUnknownWordOccurrences() }
        playerController.play()
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
        val sentenceMode = readAloud && !settingsRepository.hardWordModeAutoTranslateEnabled.value
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
     * sentence it appears in is read (English then Hebrew, same as the manual trigger's own
     * sentence mode - sentence translations aren't cached, matching existing trigger behavior).
     */
    private suspend fun speakAutoTranslatedWord(occurrence: WordTiming) {
        // The pause below, and the eventual resume at the end of translateAndSpeak(), are both the
        // app's own doing - not a user gesture - but PlaybackService's MediaSession callback can't
        // tell the difference on its own (see TriggerEventBus.suppressTriggerDetection). Without
        // this, a narration that finishes quickly (a short word, fast TTS) can land inside the
        // trigger's own pause-then-quick-resume window and get misread as the user triggering a
        // fresh translation on top of the one auto-translate just read.
        playerController.setSuppressTriggerDetection(true)
        try {
            playerController.pause()
            _uiState.update { current ->
                if (current is PlayerScreenState.Ready) current.copy(transcriptVisible = true) else current
            }
            if (settingsRepository.hardWordModeAutoTranslateEnabled.value) {
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
                translateAndSpeak(sentence.fullText, speakEnglishFirst = true)
            }
        } finally {
            playerController.setSuppressTriggerDetection(false)
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
        // TriggerEventBus is app-scoped, not tied to this ViewModel's lifecycle - clear the flag
        // explicitly so leaving the Player screen mid-overlay can't strand it stuck true, which
        // would silently break resuming playback from any source until the app restarts.
        playerController.setTranslationOverlayActive(false)
    }
}
