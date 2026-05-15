package com.bird.starryskysudoku.data.dao

import androidx.room.*
import com.bird.starryskysudoku.data.entity.HistoryEntity

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): HistoryEntity?

    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun getCount(): Int

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 20)")
    suspend fun trimToLimit()
}
