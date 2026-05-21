package com.bird.starryskysudoku.data.entity

import android.content.ContentValues
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bird.starryskysudoku.data.provider.GameResultContract

@Entity(tableName = "game_result")
data class GameResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id") val mId: Long = 0,
    @ColumnInfo(name = "level") val mLevel: Int,
    @ColumnInfo(name = "elapsed_seconds") val mElapsedSeconds: Int,
    @ColumnInfo(name = "remaining_seconds") val mRemainingSeconds: Int,
    @ColumnInfo(name = "completed") val mCompleted: Int,
    @ColumnInfo(name = "created_at") val mCreatedAt: Long
) {
    companion object {
        fun fromContentValues(values: ContentValues): GameResultEntity {
            val level = values.getAsInteger(GameResultContract.Results.COLUMN_LEVEL)
                ?: throw IllegalArgumentException("level is required")
            val elapsedSeconds = values.getAsInteger(GameResultContract.Results.COLUMN_ELAPSED_SECONDS)
                ?: throw IllegalArgumentException("elapsed_seconds is required")
            val remainingSeconds = values.getAsInteger(GameResultContract.Results.COLUMN_REMAINING_SECONDS) ?: 0
            val completed = values.getAsInteger(GameResultContract.Results.COLUMN_COMPLETED) ?: 0
            val createdAt = values.getAsLong(GameResultContract.Results.COLUMN_CREATED_AT)
                ?: System.currentTimeMillis()

            require(level > 0) { "level must be positive" }
            require(elapsedSeconds >= 0) { "elapsed_seconds must not be negative" }
            require(remainingSeconds >= 0) { "remaining_seconds must not be negative" }

            return GameResultEntity(
                mLevel = level,
                mElapsedSeconds = elapsedSeconds,
                mRemainingSeconds = remainingSeconds,
                mCompleted = if (completed == 0) 0 else 1,
                mCreatedAt = createdAt
            )
        }
    }
}
