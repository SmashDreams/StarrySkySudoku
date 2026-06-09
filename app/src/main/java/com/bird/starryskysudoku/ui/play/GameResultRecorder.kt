package com.bird.starryskysudoku.ui.play

import android.util.Log
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.dao.GameResultDao
import com.bird.starryskysudoku.data.entity.GameResultEntity
import com.bird.starryskysudoku.timer.CountdownTimerContract

class GameResultRecorder(
    private val mGameResultDao: GameResultDao
) {
    fun save(
        level: Int,
        remainingSeconds: Int,
        completed: Boolean,
        username: String
    ): Boolean {
        val elapsedSeconds = (CountdownTimerContract.DEFAULT_TOTAL_SECONDS - remainingSeconds)
            .coerceAtLeast(0)
        return try {
            val safeUsername = username.trim().ifEmpty { LauncherSessionReader.GUEST_USERNAME }
            mGameResultDao.insert(
                GameResultEntity(
                    mLevel = level,
                    mElapsedSeconds = elapsedSeconds,
                    mRemainingSeconds = remainingSeconds,
                    mCompleted = if (completed) 1 else 0,
                    mCreatedAt = System.currentTimeMillis(),
                    mUsername = safeUsername
                )
            )
            true
        } catch (exception: RuntimeException) {
            Log.w(TAG, "保存战绩失败", exception)
            false
        }
    }

    private companion object {
        const val TAG = "GameResultRecorder"
    }
}
