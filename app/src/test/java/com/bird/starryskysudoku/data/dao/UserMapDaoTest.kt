package com.bird.starryskysudoku.data.dao

import androidx.room.Room
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.UserMapEntity
import com.bird.starryskysudoku.data.repository.PlayRepository
import com.bird.starryskysudoku.ui.play.PlayViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UserMapDaoTest {
    private val mDb = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun getAllForUserKeepsRowsPerUserOrderedByPassNumber() = runBlocking {
        val dao = mDb.userMapDao()
        dao.insertAll(
            listOf(
                UserMapEntity("alice", 2, "未通关", 0),
                UserMapEntity("bob", 1, "待通关", 3),
                UserMapEntity("alice", 1, "待通关", 1)
            )
        )

        val rows = dao.getAllForUser("alice")

        assertEquals(listOf(1, 2), rows.map { it.mPassNum })
        assertEquals(listOf("待通关", "未通关"), rows.map { it.mStatus })
    }

    @Test
    fun insertAllIgnoresExistingUserPassAndUpdatesSingleUserRow() = runBlocking {
        val dao = mDb.userMapDao()
        dao.insertAll(
            listOf(
                UserMapEntity("alice", 1, "待通关", 0),
                UserMapEntity("alice", 1, "已通关", 8),
                UserMapEntity("bob", 1, "待通关", 0)
            )
        )

        dao.updateStatus("alice", 1, "已通关")
        dao.updatePlayTime("alice", 1, 1)

        val alice = requireNotNull(dao.getByUserAndPass("alice", 1))
        val bob = requireNotNull(dao.getByUserAndPass("bob", 1))
        assertEquals("已通关", alice.mStatus)
        assertEquals(1, alice.mPlayTime)
        assertEquals("待通关", bob.mStatus)
        assertEquals(0, bob.mPlayTime)
    }

    @Test
    fun updatePassStatusPreservesCompletedNextPassWhenReplayingPreviousPass() = runBlocking {
        val dao = mDb.userMapDao()
        dao.insertAll(
            listOf(
                UserMapEntity("alice", 1, "待通关", 0),
                UserMapEntity("alice", 2, "已通关", 1)
            )
        )

        PlayViewModel(PlayRepository(mDb)).updatePassStatus("alice", 1, 2)

        val firstPass = requireNotNull(dao.getByUserAndPass("alice", 1))
        val secondPass = requireNotNull(dao.getByUserAndPass("alice", 2))
        assertEquals("已通关", firstPass.mStatus)
        assertEquals(1, firstPass.mPlayTime)
        assertEquals("已通关", secondPass.mStatus)
    }
}
