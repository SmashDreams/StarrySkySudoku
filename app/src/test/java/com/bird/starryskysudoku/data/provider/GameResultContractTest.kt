package com.bird.starryskysudoku.data.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class GameResultContractTest {

    @Test
    fun contentUriUsesStableAuthorityAndResultsPath() {
        assertEquals("com.bird.starryskysudoku.provider", GameResultContract.AUTHORITY)
        assertEquals("results", GameResultContract.Results.PATH)
        assertEquals("content://com.bird.starryskysudoku.provider/results", GameResultContract.Results.CONTENT_URI_STRING)
        assertEquals("vnd.android.cursor.dir/vnd.com.bird.starryskysudoku.result", GameResultContract.Results.CONTENT_TYPE)
        assertEquals("vnd.android.cursor.item/vnd.com.bird.starryskysudoku.result", GameResultContract.Results.CONTENT_ITEM_TYPE)
    }

    @Test
    fun resultColumnsExposeProviderSchema() {
        assertEquals("_id", GameResultContract.Results.COLUMN_ID)
        assertEquals("level", GameResultContract.Results.COLUMN_LEVEL)
        assertEquals("elapsed_seconds", GameResultContract.Results.COLUMN_ELAPSED_SECONDS)
        assertEquals("remaining_seconds", GameResultContract.Results.COLUMN_REMAINING_SECONDS)
        assertEquals("completed", GameResultContract.Results.COLUMN_COMPLETED)
        assertEquals("created_at", GameResultContract.Results.COLUMN_CREATED_AT)
    }
}
