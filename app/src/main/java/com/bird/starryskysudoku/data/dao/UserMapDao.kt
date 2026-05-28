package com.bird.starryskysudoku.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bird.starryskysudoku.data.entity.UserMapEntity

@Dao
interface UserMapDao {
    @Query("SELECT * FROM user_map WHERE username = :username ORDER BY pass_num")
    suspend fun getAllForUser(username: String): List<UserMapEntity>

    @Query("SELECT * FROM user_map WHERE username = :username AND pass_num = :passNum")
    suspend fun getByUserAndPass(username: String, passNum: Int): UserMapEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<UserMapEntity>)

    @Query("UPDATE user_map SET status = :status WHERE username = :username AND pass_num = :passNum")
    suspend fun updateStatus(username: String, passNum: Int, status: String)

    @Query("UPDATE user_map SET play_time = :times WHERE username = :username AND pass_num = :passNum")
    suspend fun updatePlayTime(username: String, passNum: Int, times: Int)
}
