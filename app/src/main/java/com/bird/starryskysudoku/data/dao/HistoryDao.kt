package com.bird.starryskysudoku.data.dao

import androidx.room.*
import com.bird.starryskysudoku.data.entity.HistoryEntity

@Dao
interface HistoryDao {
    // 撤销总是按最近一次操作回退，所以这里固定按主键倒序取一条。
    @Query("SELECT * FROM history ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): HistoryEntity?

    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun getCount(): Int

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY id DESC LIMIT 20)")
    // 单局历史只保留最近二十步，足够支撑撤销又不会让表持续膨胀。
    suspend fun trimToLimit()
}
