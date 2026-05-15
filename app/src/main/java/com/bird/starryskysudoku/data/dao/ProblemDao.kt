package com.bird.starryskysudoku.data.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ProblemDao {
    @Query("SELECT value FROM problem WHERE pass_num = :passNum ORDER BY rowid")
    suspend fun getValuesForLevel(passNum: Int): List<Int>
}
