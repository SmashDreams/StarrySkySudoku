package com.bird.starryskysudoku.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class GameResultEntityTest {

    @Test
    fun entityStoresProvidedFieldsDirectly() {
        val entity = GameResultEntity(
            mLevel = 99,
            mElapsedSeconds = 700,
            mRemainingSeconds = 700,
            mCompleted = 1,
            mCreatedAt = 1_800_000_000_000L,
            mUsername = "alice"
        )

        assertEquals(99, entity.mLevel)
        assertEquals(700, entity.mElapsedSeconds)
        assertEquals(700, entity.mRemainingSeconds)
        assertEquals(1, entity.mCompleted)
        assertEquals(1_800_000_000_000L, entity.mCreatedAt)
        assertEquals("alice", entity.mUsername)
    }

    @Test
    fun entityDefaultsUsernameToGuest() {
        val entity = GameResultEntity(
            mLevel = 4,
            mElapsedSeconds = 40,
            mRemainingSeconds = 560,
            mCompleted = 0,
            mCreatedAt = 1_800_000_000_003L
        )

        assertEquals("guest", entity.mUsername)
    }

    @Test
    fun entityDoesNotExposeContentValuesFactory() {
        val source = File("src/main/java/com/bird/starryskysudoku/data/entity/GameResultEntity.kt").readText()

        assertFalse(source.contains("fun fromContentValues("))
    }
}
