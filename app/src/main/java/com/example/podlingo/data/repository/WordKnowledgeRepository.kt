package com.example.podlingo.data.repository

import com.example.podlingo.core.WordNormalizer
import com.example.podlingo.data.local.dao.WordKnowledgeDao
import com.example.podlingo.data.local.entity.WordKnowledgeEntity
import com.example.podlingo.data.local.entity.WordKnowledgeStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * The user's app-wide vocabulary profile: which words they've said they don't know (and so get
 * auto-translated on sight - see [com.example.podlingo.ui.player.PlayerViewModel]) versus words
 * they've confirmed knowing, either by not flagging them during calibration or by answering
 * correctly in the end-of-episode quiz. Every word is translated at most once ever - translations
 * are cached in [WordKnowledgeEntity.hebrewTranslation] and only fetched lazily, never
 * pre-fetched for the whole calibration list.
 */
@Singleton
class WordKnowledgeRepository @Inject constructor(
    private val dao: WordKnowledgeDao,
    private val translationRepository: TranslationRepository,
) {

    /** Keyed by normalized word - only words with an existing row are present. */
    suspend fun getStatuses(words: Collection<String>): Map<String, WordKnowledgeEntity> {
        val normalized = words.map(WordNormalizer::normalize).distinct()
        if (normalized.isEmpty()) return emptyMap()
        return dao.getByWords(normalized).associateBy { it.word }
    }

    suspend fun markUnknown(word: String) = upsertStatus(word, WordKnowledgeStatus.UNKNOWN)

    suspend fun markKnown(word: String) = upsertStatus(word, WordKnowledgeStatus.KNOWN)

    private suspend fun upsertStatus(word: String, status: WordKnowledgeStatus) {
        val normalized = WordNormalizer.normalize(word)
        if (normalized.isEmpty()) return
        val existing = dao.getByWords(listOf(normalized)).firstOrNull()
        val now = System.currentTimeMillis()
        // Only a real KNOWN -> UNKNOWN (or brand-new) transition counts as "added" - re-affirming
        // an already-unknown word (e.g. a repeat wrong quiz answer) shouldn't bump its place in the
        // last-added sort.
        val addedAt = if (status == WordKnowledgeStatus.UNKNOWN && existing?.status != WordKnowledgeStatus.UNKNOWN) {
            now
        } else {
            existing?.addedAtEpochMs ?: now
        }
        dao.upsertAll(
            listOf(
                WordKnowledgeEntity(
                    word = normalized,
                    status = status,
                    hebrewTranslation = existing?.hebrewTranslation,
                    updatedAtEpochMs = now,
                    addedAtEpochMs = addedAt,
                ),
            ),
        )
    }

    /** Cache hit -> pure DB read, no network. Cache miss -> one translation call, then persisted forever. Never throws; returns null on failure. */
    suspend fun getOrFetchTranslation(word: String): String? {
        val normalized = WordNormalizer.normalize(word)
        if (normalized.isEmpty()) return null
        val existing = dao.getByWords(listOf(normalized)).firstOrNull()
        existing?.hebrewTranslation?.let { return it }

        val translated = translationRepository.translateToHebrew(word).getOrNull() ?: return null
        dao.upsertAll(
            listOf(
                WordKnowledgeEntity(
                    word = normalized,
                    status = existing?.status ?: WordKnowledgeStatus.UNKNOWN,
                    hebrewTranslation = translated,
                    updatedAtEpochMs = System.currentTimeMillis(),
                    addedAtEpochMs = existing?.addedAtEpochMs ?: System.currentTimeMillis(),
                ),
            ),
        )
        return translated
    }

    suspend fun sampleDistractors(word: String, count: Int): List<String> =
        dao.sampleRandomTranslations(WordNormalizer.normalize(word), count)

    /** Permanently forgets these words - both status and any cached translation are cleared, so they can be asked about again in future calibration/quizzes. */
    suspend fun deleteWords(words: Collection<String>) {
        val normalized = words.map(WordNormalizer::normalize).distinct()
        if (normalized.isEmpty()) return
        dao.deleteByWords(normalized)
    }

    /** For the Settings "words you don't know" list. */
    fun observeUnknownWords(): Flow<List<WordKnowledgeEntity>> = dao.getUnknownWordsFlow()

    /** For the Settings "words you know" list. */
    fun observeKnownWords(): Flow<List<WordKnowledgeEntity>> = dao.getKnownWordsFlow()
}
