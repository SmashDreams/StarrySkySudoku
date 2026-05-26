package com.bird.starryskysudoku.data.provider

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameResultContractTest {

    @Test
    fun contentUriUsesStableAuthorityAndResultsPath() {
        assertEquals("com.bird.starryskysudoku.provider", GameResultContract.AUTHORITY)
        assertEquals("com.bird.starryskysudoku.permission.WRITE_RESULTS", GameResultContract.WRITE_PERMISSION)
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
        assertEquals("username", GameResultContract.Results.COLUMN_USERNAME)
        assertEquals("username=?", GameResultContract.Results.selectionForUsername())
    }

    @Test
    fun toContentValuesCanIncludeUsernameWithoutBreakingDefaultGuest() {
        val guestValues = GameResultContract.Results.toContentValues(
            level = 1,
            elapsedSeconds = 10,
            remainingSeconds = 590,
            completed = false,
            createdAt = 1_800_000_000_000L
        )
        val userValues = GameResultContract.Results.toContentValues(
            level = 2,
            elapsedSeconds = 20,
            remainingSeconds = 580,
            completed = true,
            createdAt = 1_800_000_000_001L,
            username = "alice"
        )

        assertEquals("guest", guestValues.getAsString(GameResultContract.Results.COLUMN_USERNAME))
        assertEquals("alice", userValues.getAsString(GameResultContract.Results.COLUMN_USERNAME))
    }
}
