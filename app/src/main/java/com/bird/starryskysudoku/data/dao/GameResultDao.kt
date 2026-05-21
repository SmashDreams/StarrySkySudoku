package com.bird.starryskysudoku.data.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bird.starryskysudoku.data.entity.GameResultEntity

@Dao
interface GameResultDao {
    @Query("SELECT _id, level, elapsed_seconds, remaining_seconds, completed, created_at FROM game_result ORDER BY created_at DESC")
    fun queryAll(): Cursor

    @Query("SELECT _id, level, elapsed_seconds, remaining_seconds, completed, created_at FROM game_result WHERE _id = :id")
    fun queryById(id: Long): Cursor

    @Insert
    fun insert(result: GameResultEntity): Long

    @Query("DELETE FROM game_result WHERE _id = :id")
    fun deleteById(id: Long): Int
}
