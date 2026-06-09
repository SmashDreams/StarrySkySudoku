package com.bird.starryskysudoku.ui.play

import androidx.room.Room
import com.bird.starryskysudoku.data.database.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GameResultRecorderTest {
    private val mDb = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun savePersistsGameResultThroughDao() {
        val recorder = GameResultRecorder(mDb.gameResultDao())

        assertTrue(
            recorder.save(
                level = 7,
                remainingSeconds = 543,
                completed = true,
                username = "alice"
            )
        )

        mDb.gameResultDao().queryByUsername("alice").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(7, cursor.getInt(cursor.getColumnIndexOrThrow("level")))
            assertEquals(57, cursor.getInt(cursor.getColumnIndexOrThrow("elapsed_seconds")))
            assertEquals(543, cursor.getInt(cursor.getColumnIndexOrThrow("remaining_seconds")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("completed")))
            assertEquals("alice", cursor.getString(cursor.getColumnIndexOrThrow("username")))
        }
    }
}
