package com.bird.starryskysudoku.data.dao

import androidx.room.Room
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.GameResultEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GameResultDaoTest {
    private val mDb = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun queryByIdIncludesPersistedUsernameColumn() {
        val dao = mDb.gameResultDao()
        val id = dao.insert(
            GameResultEntity.fromFields(
                level = 5,
                elapsedSeconds = 120,
                remainingSeconds = 480,
                completed = true,
                createdAt = 1_800_000_000_000L,
                username = "alice"
            )
        )

        dao.queryById(id).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("alice", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        }
    }

    @Test
    fun queryByUsernameReturnsOnlyMatchingUsername() {
        val dao = mDb.gameResultDao()
        dao.insert(
            GameResultEntity.fromFields(
                level = 1,
                elapsedSeconds = 60,
                remainingSeconds = 540,
                completed = false,
                createdAt = 1_800_000_000_000L,
                username = "guest"
            )
        )
        dao.insert(
            GameResultEntity.fromFields(
                level = 2,
                elapsedSeconds = 120,
                remainingSeconds = 480,
                completed = true,
                createdAt = 1_800_000_000_001L,
                username = "alice"
            )
        )

        dao.queryByUsername("alice").use { cursor ->
            assertEquals(1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("alice", cursor.getString(cursor.getColumnIndexOrThrow("username")))
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("level")))
        }
    }
}
