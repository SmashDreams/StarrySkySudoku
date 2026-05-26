package com.bird.starryskysudoku.data.entity

import android.content.ContentValues
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.provider.GameResultContract

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
) {
    companion object {
        fun fromContentValues(values: ContentValues): GameResultEntity {
            /*
             * 内容提供器是跨进程入口，所有外部输入必须在落库前重新校验。
             * 契约辅助方法只是便捷构造器，不能作为可信边界。
             */
            val level = values.getAsInteger(GameResultContract.Results.COLUMN_LEVEL)
                ?: throw IllegalArgumentException("level is required")
            val elapsedSeconds = values.getAsInteger(GameResultContract.Results.COLUMN_ELAPSED_SECONDS)
                ?: throw IllegalArgumentException("elapsed_seconds is required")
            val remainingSeconds = values.getAsInteger(GameResultContract.Results.COLUMN_REMAINING_SECONDS) ?: 0
            val completed = values.getAsInteger(GameResultContract.Results.COLUMN_COMPLETED) ?: 0
            val createdAt = values.getAsLong(GameResultContract.Results.COLUMN_CREATED_AT)
                ?: System.currentTimeMillis()

            return fromFields(
                level = level,
                elapsedSeconds = elapsedSeconds,
                remainingSeconds = remainingSeconds,
                completed = completed != 0,
                createdAt = createdAt
            )
        }

        fun fromFields(
            level: Int,
            elapsedSeconds: Int,
            remainingSeconds: Int,
            completed: Boolean,
            createdAt: Long = System.currentTimeMillis(),
            username: String = LauncherSessionReader.GUEST_USERNAME
        ): GameResultEntity {
            /*
             * 字段级校验不依赖系统框架类，便于单元测试覆盖边界条件。
             * 通过公开内容提供器写入的数据最终都会走到这里。
             */
            require(level in GameResultContract.Results.MIN_LEVEL..GameResultContract.Results.MAX_LEVEL) {
                "level must be in ${GameResultContract.Results.MIN_LEVEL}..${GameResultContract.Results.MAX_LEVEL}"
            }
            require(elapsedSeconds >= 0) { "elapsed_seconds must not be negative" }
            require(remainingSeconds >= 0) { "remaining_seconds must not be negative" }
            require(elapsedSeconds <= GameResultContract.Results.MAX_GAME_SECONDS) {
                "elapsed_seconds must not exceed ${GameResultContract.Results.MAX_GAME_SECONDS}"
            }
            require(remainingSeconds <= GameResultContract.Results.MAX_GAME_SECONDS) {
                "remaining_seconds must not exceed ${GameResultContract.Results.MAX_GAME_SECONDS}"
            }
            require(elapsedSeconds + remainingSeconds <= GameResultContract.Results.MAX_GAME_SECONDS) {
                "elapsed_seconds + remaining_seconds must not exceed one game window"
            }
            require(createdAt > 0L) { "created_at must be positive" }
            val safeUsername = username.trim().ifEmpty { LauncherSessionReader.GUEST_USERNAME }

            return GameResultEntity(
                mLevel = level,
                mElapsedSeconds = elapsedSeconds,
                mRemainingSeconds = remainingSeconds,
                mCompleted = if (completed) 1 else 0,
                mCreatedAt = createdAt,
                mUsername = safeUsername
            )
        }
    }
}
