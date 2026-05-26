package com.bird.starryskysudoku.data.entity

import com.bird.starryskysudoku.data.provider.GameResultContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameResultEntityTest {

    @Test
    fun fromFieldsAcceptsOnlyGameBoundedResult() {
        val entity = GameResultEntity.fromFields(
            level = 40,
            elapsedSeconds = 300,
            remainingSeconds = 300,
            completed = true,
            createdAt = 1_800_000_000_000L
        )

        assertEquals(40, entity.mLevel)
        assertEquals(300, entity.mElapsedSeconds)
        assertEquals(300, entity.mRemainingSeconds)
        assertEquals(1, entity.mCompleted)
        assertEquals(1_800_000_000_000L, entity.mCreatedAt)
        assertEquals("guest", entity.mUsername)
    }

    @Test
    fun fromFieldsRejectsOutOfRangeLevelAndTimerData() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultEntity.fromFields(
                level = 41,
                elapsedSeconds = 10,
                remainingSeconds = 590,
                completed = true
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            GameResultEntity.fromFields(
                level = 1,
                elapsedSeconds = 601,
                remainingSeconds = 0,
                completed = true
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            GameResultEntity.fromFields(
                level = 1,
                elapsedSeconds = 300,
                remainingSeconds = 301,
                completed = true
            )
        }
    }

    @Test
    fun fromContentValuesPersistsUsernameAndBlanksFallbackToGuest() {
        val values = GameResultContract.Results.toContentValues(
            level = 3,
            elapsedSeconds = 30,
            remainingSeconds = 570,
            completed = true,
            createdAt = 1_800_000_000_002L,
            username = " alice "
        )
        val blankValues = GameResultContract.Results.toContentValues(
            level = 4,
            elapsedSeconds = 40,
            remainingSeconds = 560,
            completed = false,
            createdAt = 1_800_000_000_003L,
            username = "   "
        )

        assertEquals("alice", GameResultEntity.fromContentValues(values).mUsername)
        assertEquals("guest", GameResultEntity.fromContentValues(blankValues).mUsername)
    }
}
