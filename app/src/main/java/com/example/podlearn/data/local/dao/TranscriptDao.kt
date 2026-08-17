package com.example.podlearn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.podlearn.data.local.entity.SentenceEntity
import com.example.podlearn.data.local.entity.WordEntity

@Dao
interface TranscriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSentences(sentences: List<SentenceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    @Query("DELETE FROM sentences WHERE episodeId = :episodeId")
    suspend fun clearSentencesForEpisode(episodeId: String)

    @Query("DELETE FROM words WHERE episodeId = :episodeId")
    suspend fun clearWordsForEpisode(episodeId: String)

    @Transaction
    suspend fun replaceTranscript(episodeId: String, sentences: List<SentenceEntity>, words: List<WordEntity>) {
        clearWordsForEpisode(episodeId)
        clearSentencesForEpisode(episodeId)
        insertSentences(sentences)
        insertWords(words)
    }

    @Query("SELECT * FROM words WHERE episodeId = :episodeId ORDER BY startMs ASC")
    suspend fun getWordsForEpisode(episodeId: String): List<WordEntity>

    @Query("SELECT * FROM sentences WHERE episodeId = :episodeId ORDER BY startMs ASC")
    suspend fun getSentencesForEpisode(episodeId: String): List<SentenceEntity>

    @Query("SELECT * FROM sentences WHERE id = :sentenceId")
    suspend fun getSentence(sentenceId: String): SentenceEntity?
}
