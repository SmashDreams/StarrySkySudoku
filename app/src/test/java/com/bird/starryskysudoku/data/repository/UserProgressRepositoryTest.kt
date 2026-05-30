package com.bird.starryskysudoku.data.repository

import androidx.room.Room
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UserProgressRepositoryTest {
    private val mDb = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun ensureUserMapCreatesFortyLevelsAndTrimsBlankUsername() = runBlocking {
        val repository = UserProgressRepository(mDb)

        val username = repository.ensureUserMap("  alice  ")

        assertEquals("alice", username)
        val rows = mDb.userMapDao().getAllForUser("alice")
        assertEquals(40, rows.size)
        assertEquals(1, rows.first().mPassNum)
        assertEquals(PassStatus.TODO, rows.first().mStatus)
        assertEquals(0, rows.first().mPlayTime)
        assertEquals(PassStatus.LOCKED, rows[1].mStatus)
        assertEquals(PassStatus.LOCKED, rows.last().mStatus)
    }

    @Test
    fun ensureUserMapFallsBackToGuestForBlankUsernameAndDoesNotOverwriteExistingRows() = runBlocking {
        seedBaseMap()
        val repository = UserProgressRepository(mDb)

        repository.ensureUserMap("   ")
        mDb.userMapDao().updateStatus(LauncherSessionReader.GUEST_USERNAME, 1, PassStatus.COMPLETED)
        repository.ensureUserMap(LauncherSessionReader.GUEST_USERNAME)

        val first = requireNotNull(mDb.userMapDao().getByUserAndPass(LauncherSessionReader.GUEST_USERNAME, 1))
        assertEquals(PassStatus.COMPLETED, first.mStatus)
    }

    @Test
    fun completePassIncrementsPlayTimeAndKeepsCompletedNextPass() = runBlocking {
        seedBaseMap()
        val repository = UserProgressRepository(mDb)
        repository.ensureUserMap("alice")
        mDb.userMapDao().updateStatus("alice", 2, PassStatus.COMPLETED)
        mDb.userMapDao().updatePlayTime("alice", 2, 4)

        repository.completePass("alice", 1, 2)

        val first = requireNotNull(mDb.userMapDao().getByUserAndPass("alice", 1))
        val second = requireNotNull(mDb.userMapDao().getByUserAndPass("alice", 2))
        assertEquals(PassStatus.COMPLETED, first.mStatus)
        assertEquals(1, first.mPlayTime)
        assertEquals(PassStatus.COMPLETED, second.mStatus)
        assertEquals(4, second.mPlayTime)
    }

    private fun seedBaseMap() {
        val db = mDb.openHelper.writableDatabase
        db.execSQL("INSERT INTO user_map(username, pass_num, status, play_time) VALUES('guest', 1, '待通关', 7)")
        db.execSQL("INSERT INTO user_map(username, pass_num, status, play_time) VALUES('guest', 2, '未通关', 2)")
        db.execSQL("INSERT INTO user_map(username, pass_num, status, play_time) VALUES('guest', 3, '未通关', 0)")
    }
}
