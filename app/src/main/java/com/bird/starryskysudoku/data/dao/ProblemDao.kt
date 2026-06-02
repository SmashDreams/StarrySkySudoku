package com.bird.starryskysudoku.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ProblemDao {
    // 题面按原始入库顺序读取，交给上层还原成九乘九棋盘。
    @Query("SELECT value FROM problem WHERE pass_num = :passNum ORDER BY rowid")
    suspend fun getValuesForLevel(passNum: Int): List<Int>
}
