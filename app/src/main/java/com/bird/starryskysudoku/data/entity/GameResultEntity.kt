package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bird.starryskysudoku.account.LauncherSessionReader

@Entity(tableName = "game_result")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id") val mId: Long = 0,
    @ColumnInfo(name = "level") val mLevel: Int,
    @ColumnInfo(name = "elapsed_seconds") val mElapsedSeconds: Int,
    @ColumnInfo(name = "remaining_seconds") val mRemainingSeconds: Int,
    @ColumnInfo(name = "completed") val mCompleted: Int,
    @ColumnInfo(name = "created_at") val mCreatedAt: Long,
    @ColumnInfo(name = "username") val mUsername: String = LauncherSessionReader.GUEST_USERNAME
)
