package com.bird.starryskysudoku.data.provider

import android.content.ContentValues
import android.net.Uri

object GameResultContract {
    const val AUTHORITY = "com.bird.starryskysudoku.provider"
    const val CONTENT_URI_BASE = "content://$AUTHORITY"

    object Results {
        const val PATH = "results"
        const val CONTENT_URI_STRING = "$CONTENT_URI_BASE/$PATH"
        val CONTENT_URI: Uri = Uri.parse(CONTENT_URI_STRING)

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.bird.starryskysudoku.result"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.bird.starryskysudoku.result"

        const val COLUMN_ID = "_id"
        const val COLUMN_LEVEL = "level"
        const val COLUMN_ELAPSED_SECONDS = "elapsed_seconds"
        const val COLUMN_REMAINING_SECONDS = "remaining_seconds"
        const val COLUMN_COMPLETED = "completed"
        const val COLUMN_CREATED_AT = "created_at"

        fun toContentValues(
            level: Int,
            elapsedSeconds: Int,
            remainingSeconds: Int,
            completed: Boolean,
            createdAt: Long = System.currentTimeMillis()
        ): ContentValues {
            return ContentValues().apply {
                put(COLUMN_LEVEL, level)
                put(COLUMN_ELAPSED_SECONDS, elapsedSeconds)
                put(COLUMN_REMAINING_SECONDS, remainingSeconds)
                put(COLUMN_COMPLETED, if (completed) 1 else 0)
                put(COLUMN_CREATED_AT, createdAt)
            }
        }
    }
}
