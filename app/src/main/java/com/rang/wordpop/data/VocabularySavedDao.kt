package com.rang.wordpop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularySavedDao {

    @Insert
    suspend fun insert(item: VocabularySaved)

    @Delete
    suspend fun delete(item: VocabularySaved)

    @Query("SELECT * FROM vocabulary_saved ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<VocabularySaved>>

    @Query("UPDATE vocabulary_saved SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("SELECT * FROM vocabulary_saved WHERE text = :text AND type = :type LIMIT 1")
    suspend fun findByTextAndType(text: String, type: String): VocabularySaved?
}
