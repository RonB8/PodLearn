package com.example.podlingo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.podlingo.data.local.entity.WordKnowledgeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordKnowledgeDao {

    @Query("SELECT * FROM word_knowledge WHERE word IN (:words)")
    suspend fun getByWords(words: List<String>): List<WordKnowledgeEntity>

    /** For the Settings "words you don't know" list - alphabetical so it's scannable/editable. */
    @Query("SELECT * FROM word_knowledge WHERE status = 'UNKNOWN' ORDER BY word ASC")
    fun getUnknownWordsFlow(): Flow<List<WordKnowledgeEntity>>

    /** For the Settings "words you know" list - alphabetical so it's scannable/editable. */
    @Query("SELECT * FROM word_knowledge WHERE status = 'KNOWN' ORDER BY word ASC")
    fun getKnownWordsFlow(): Flow<List<WordKnowledgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<WordKnowledgeEntity>)

    /** Permanently forgets these words - status and cached translation both gone. */
    @Query("DELETE FROM word_knowledge WHERE word IN (:words)")
    suspend fun deleteByWords(words: List<String>)

    /** Wrong-answer pool for the end-of-episode quiz - real translations of other words the user has already looked up. */
    @Query(
        "SELECT hebrewTranslation FROM word_knowledge " +
            "WHERE hebrewTranslation IS NOT NULL AND word != :excludeWord ORDER BY RANDOM() LIMIT :limit",
    )
    suspend fun sampleRandomTranslations(excludeWord: String, limit: Int): List<String>
}
