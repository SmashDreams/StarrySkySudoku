package com.bird.starryskysudoku.data.provider

import android.content.ContentValues
import android.net.Uri
import com.bird.starrysky.contracts.SharedGameResultsContract
import com.bird.starrysky.contracts.SharedSessionContract

object GameResultContract {
    // 本项目对外共享战绩时完全对齐公共契约，避免两个应用之间字段漂移。
    const val AUTHORITY = SharedGameResultsContract.AUTHORITY
    const val READ_PERMISSION = SharedGameResultsContract.READ_PERMISSION
    const val WRITE_PERMISSION = SharedGameResultsContract.WRITE_PERMISSION
    const val CONTENT_URI_BASE = SharedGameResultsContract.CONTENT_URI_BASE

    object Results {
        // 这些范围和字段名都直接跟随公共契约，避免本应用和宿主对有效数据理解不一致。
        const val MIN_LEVEL = SharedGameResultsContract.Results.MIN_LEVEL
        const val MAX_LEVEL = SharedGameResultsContract.Results.MAX_LEVEL
        const val MAX_GAME_SECONDS = SharedGameResultsContract.Results.MAX_GAME_SECONDS

        const val PATH = SharedGameResultsContract.Results.PATH
        const val CONTENT_URI_STRING = SharedGameResultsContract.Results.CONTENT_URI_STRING
        val CONTENT_URI: Uri by lazy { SharedGameResultsContract.Results.CONTENT_URI }

        const val CONTENT_TYPE = SharedGameResultsContract.Results.CONTENT_TYPE
        const val CONTENT_ITEM_TYPE = SharedGameResultsContract.Results.CONTENT_ITEM_TYPE

        const val COLUMN_ID = SharedGameResultsContract.Results.COLUMN_ID
        const val COLUMN_LEVEL = SharedGameResultsContract.Results.COLUMN_LEVEL
        const val COLUMN_ELAPSED_SECONDS = SharedGameResultsContract.Results.COLUMN_ELAPSED_SECONDS
        const val COLUMN_REMAINING_SECONDS = SharedGameResultsContract.Results.COLUMN_REMAINING_SECONDS
        const val COLUMN_COMPLETED = SharedGameResultsContract.Results.COLUMN_COMPLETED
        const val COLUMN_CREATED_AT = SharedGameResultsContract.Results.COLUMN_CREATED_AT
        const val COLUMN_USERNAME = SharedGameResultsContract.Results.COLUMN_USERNAME
        const val SORT_NEWEST_FIRST = SharedGameResultsContract.Results.SORT_NEWEST_FIRST

        fun selectionForUsername(): String = SharedGameResultsContract.Results.selectionForUsername()

        fun toContentValues(
            level: Int,
            elapsedSeconds: Int,
            remainingSeconds: Int,
            completed: Boolean,
            createdAt: Long = System.currentTimeMillis(),
            username: String = SharedSessionContract.GUEST_USERNAME
        ): ContentValues {
            // 统一通过共享契约生成内容值，确保字段名和默认值保持一致。
            return SharedGameResultsContract.Results.toContentValues(
                level = level,
                elapsedSeconds = elapsedSeconds,
                remainingSeconds = remainingSeconds,
                completed = completed,
                createdAt = createdAt,
                username = username
            )
        }
    }
}
