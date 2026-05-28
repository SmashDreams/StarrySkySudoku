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
    fun ensureUserMapCreatesRowsFromBaseMapAndTrimsBlankUsername() = runBlocking {
        seedBaseMap()
        val repository = UserProgressRepository(mDb)

        val username = repository.ensureUserMap("  alice  ")

        assertEquals("alice", username)
        val rows = mDb.userMapDao().getAllForUser("alice")
        assertEquals(listOf(1, 2, 3), rows.map { it.mPassNum })
        assertEquals(listOf(PassStatus.TODO, PassStatus.LOCKED, PassStatus.LOCKED), rows.map { it.mStatus })
        assertEquals(listOf(0, 0, 0), rows.map { it.mPlayTime })
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
        db.execSQL("INSERT INTO map(pass_num, status, play_time) VALUES(1, '待通关', '7')")
        db.execSQL("INSERT INTO map(pass_num, status, play_time) VALUES(2, '未通关', '2')")
        db.execSQL("INSERT INTO map(pass_num, status, play_time) VALUES(3, '未通关', '0')")
    }
}
