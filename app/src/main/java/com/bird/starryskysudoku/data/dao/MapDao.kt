package com.bird.starryskysudoku.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.bird.starryskysudoku.data.entity.MapEntity

@Dao
interface MapDao {
    @Query("SELECT * FROM map ORDER BY pass_num")
    suspend fun getAllMaps(): List<MapEntity>

    @Query("SELECT * FROM map WHERE pass_num = :passNum")
    suspend fun getMapByNum(passNum: Int): MapEntity?

    @Query("UPDATE map SET status = :status WHERE pass_num = :passNum")
    suspend fun updateStatus(passNum: Int, status: String)

    @Query("UPDATE map SET play_time = :times WHERE pass_num = :passNum")
    suspend fun updatePlayTime(passNum: Int, times: Int)
}
