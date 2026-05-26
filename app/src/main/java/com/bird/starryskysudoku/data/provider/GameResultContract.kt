package com.bird.starryskysudoku.data.provider

import android.content.ContentValues
import android.net.Uri
import com.bird.starryskysudoku.account.LauncherSessionReader

object GameResultContract {
    const val AUTHORITY = "com.bird.starryskysudoku.provider"
    const val WRITE_PERMISSION = "com.bird.starryskysudoku.permission.WRITE_RESULTS"
    const val CONTENT_URI_BASE = "content://$AUTHORITY"

    object Results {
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 40
        const val MAX_GAME_SECONDS = 600

        const val PATH = "results"
        const val CONTENT_URI_STRING = "$CONTENT_URI_BASE/$PATH"
        val CONTENT_URI: Uri by lazy { Uri.parse(CONTENT_URI_STRING) }

        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.bird.starryskysudoku.result"
        const val CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.bird.starryskysudoku.result"

        const val COLUMN_ID = "_id"
        const val COLUMN_LEVEL = "level"
        const val COLUMN_ELAPSED_SECONDS = "elapsed_seconds"
        const val COLUMN_REMAINING_SECONDS = "remaining_seconds"
        const val COLUMN_COMPLETED = "completed"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_USERNAME = "username"

        fun selectionForUsername(): String = "$COLUMN_USERNAME=?"

        fun toContentValues(
            level: Int,
            elapsedSeconds: Int,
            remainingSeconds: Int,
            completed: Boolean,
            createdAt: Long = System.currentTimeMillis(),
            username: String = LauncherSessionReader.GUEST_USERNAME
        ): ContentValues {
            /*
             * 这里仅负责按公开字段组装写入数据，方便本应用和外部调用方复用。
             * 真正的安全校验放在实体转换阶段，不能信任调用方传入的数据。
             */
            return ContentValues().apply {
                put(COLUMN_LEVEL, level)
                put(COLUMN_ELAPSED_SECONDS, elapsedSeconds)
                put(COLUMN_REMAINING_SECONDS, remainingSeconds)
                put(COLUMN_COMPLETED, if (completed) 1 else 0)
                put(COLUMN_CREATED_AT, createdAt)
                put(COLUMN_USERNAME, username)
            }
        }
    }
}
