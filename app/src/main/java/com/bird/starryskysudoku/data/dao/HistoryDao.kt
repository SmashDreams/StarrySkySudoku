package com.bird.starryskysudoku.data.dao

import androidx.room.*
import com.bird.starryskysudoku.data.entity.HistoryEntity

@Dao
interface HistoryDao {
    // 撤销总是按最近一次操作回退，所以这里固定按主键倒序取一条。
    @Query("SELECT * FROM history WHERE pass_num = :passNum AND game_session = :gameSession ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(passNum: Int, gameSession: String): HistoryEntity?

    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM history WHERE pass_num = :passNum AND game_session = :gameSession")
    suspend fun deleteForSession(passNum: Int, gameSession: String)

    @Query("DELETE FROM history WHERE pass_num = :passNum")
    suspend fun deleteForPass(passNum: Int)

    @Query("SELECT COUNT(*) FROM history WHERE pass_num = :passNum AND game_session = :gameSession")
    suspend fun getCount(passNum: Int, gameSession: String): Int

    @Query("DELETE FROM history WHERE pass_num = :passNum AND game_session = :gameSession AND id NOT IN (SELECT id FROM history WHERE pass_num = :passNum AND game_session = :gameSession ORDER BY id DESC LIMIT 20)")
    // 单局历史只保留最近二十步，足够支撑撤销又不会让表持续膨胀。
    suspend fun trimToLimit(passNum: Int, gameSession: String)
}
