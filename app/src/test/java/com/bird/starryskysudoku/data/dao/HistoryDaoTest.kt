package com.bird.starryskysudoku.data.dao

import androidx.room.Room
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.HistoryEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HistoryDaoTest {
    private val mDb = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        AppDatabase::class.java
    ).allowMainThreadQueries().build()

    @After
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun historyActsAsSingleCurrentGameUndoStack() = kotlinx.coroutines.runBlocking {
        val dao = mDb.historyDao()
        dao.insert(HistoryEntity(mRow = 0, mCol = 1, mType = 0, mValue = 3))
        dao.insert(HistoryEntity(mRow = 2, mCol = 3, mType = 1, mValue = 9))

        val latest = dao.getLatest()

        assertEquals(2, latest?.mRow)
        assertEquals(3, latest?.mCol)
        assertEquals(2, dao.getCount())
    }

    @Test
    fun clearRemovesAllHistoryForCurrentGame() = kotlinx.coroutines.runBlocking {
        val dao = mDb.historyDao()
        dao.insert(HistoryEntity(mRow = 0, mCol = 1, mType = 0, mValue = 3))
        dao.insert(HistoryEntity(mRow = 2, mCol = 3, mType = 1, mValue = 9))

        dao.clear()

        assertEquals(0, dao.getCount())
        assertNull(dao.getLatest())
    }

    @Test
    fun trimKeepsOnlyLatestTwentyRows() = kotlinx.coroutines.runBlocking {
        val dao = mDb.historyDao()
        repeat(25) { index ->
            dao.insert(HistoryEntity(mRow = index % 9, mCol = 0, mType = 0, mValue = index))
        }

        dao.trimToLimit()

        assertEquals(20, dao.getCount())
        assertTrue(requireNotNull(dao.getLatest()).mValue == 24)
    }
}
