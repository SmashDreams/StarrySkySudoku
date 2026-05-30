package com.bird.starryskysudoku.ui.play

import android.content.ContentResolver
import android.util.Log
import com.bird.starryskysudoku.data.provider.GameResultContract
import com.bird.starryskysudoku.timer.CountdownTimerContract

class GameResultRecorder(
    private val mContentResolver: ContentResolver
) {
    fun saveAndVerify(
        level: Int,
        remainingSeconds: Int,
        completed: Boolean,
        username: String
    ): Boolean {
        val elapsedSeconds = (CountdownTimerContract.DEFAULT_TOTAL_SECONDS - remainingSeconds)
            .coerceAtLeast(0)
        return try {
            val values = GameResultContract.Results.toContentValues(
                level = level,
                elapsedSeconds = elapsedSeconds,
                remainingSeconds = remainingSeconds,
                completed = completed,
                username = username
            )
            val insertedUri = mContentResolver.insert(GameResultContract.Results.CONTENT_URI, values)
                ?: return false
            mContentResolver.query(insertedUri, null, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return false
                cursor.getInt(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_LEVEL))
                cursor.getInt(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_ELAPSED_SECONDS))
                cursor.getInt(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_COMPLETED))
                cursor.getString(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_USERNAME))
            }
            true
        } catch (exception: RuntimeException) {
            Log.w(TAG, "通过内容提供器保存战绩失败", exception)
            false
        }
    }

    private companion object {
        const val TAG = "GameResultRecorder"
    }
}
