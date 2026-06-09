package com.bird.starrysky.contracts

import android.net.Uri

object SharedGameResultsContract {
    /*
     * 战绩 Provider 由数独实现、茶苑读取；共享契约避免两边手工复制字段导致漂移。
     */
    const val AUTHORITY = "${SharedGameEntryContract.PACKAGE_NAME}.provider"
    const val READ_PERMISSION = "com.bird.starryskysudoku.permission.READ_RESULTS"
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
        const val COLUMN_USERNAME = "username"
        const val COLUMN_LEVEL = "level"
        const val COLUMN_ELAPSED_SECONDS = "elapsed_seconds"
        const val COLUMN_REMAINING_SECONDS = "remaining_seconds"
        const val COLUMN_COMPLETED = "completed"
        const val COLUMN_CREATED_AT = "created_at"
        const val SORT_NEWEST_FIRST = "$COLUMN_CREATED_AT DESC"

        val PROJECTION = arrayOf(
            COLUMN_USERNAME,
            COLUMN_LEVEL,
            COLUMN_ELAPSED_SECONDS,
            COLUMN_REMAINING_SECONDS,
            COLUMN_COMPLETED,
            COLUMN_CREATED_AT
        )

        fun selectionForUsername(): String = "$COLUMN_USERNAME=?"
    }
}
