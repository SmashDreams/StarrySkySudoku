package com.bird.starryskysudoku.data.provider

import android.content.ContentUris
import androidx.room.Room
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class GameResultProviderQueryFilterTest {
    private lateinit var mDb: AppDatabase

    @Before
    fun setUpDatabase() {
        mDb = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        setDatabaseInitializerInstance(mDb)
    }

    @After
    fun tearDownDatabase() {
        setDatabaseInitializerInstance(null)
        mDb.close()
    }

    @Test
    fun queryKeepsReturningResultsWhenOptionalArgumentsAreUnexpected() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()
        mDb.gameResultDao().insert(
            com.bird.starryskysudoku.data.entity.GameResultEntity(
                mLevel = 1,
                mElapsedSeconds = 10,
                mRemainingSeconds = 590,
                mCompleted = 0,
                mCreatedAt = 1_800_000_000_002L,
                mUsername = "alice"
            )
        )

        provider.query(
            GameResultContract.Results.CONTENT_URI,
            null,
            null,
            arrayOf("ignored"),
            "level ASC"
        ).use { cursor ->
            assertNotNull(cursor)
            assertTrue(cursor.moveToFirst())
            assertEquals("alice", cursor.getString(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_USERNAME)))
        }
    }

    @Test
    fun insertIsRejectedBecauseProviderIsReadOnly() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()

        try {
            provider.insert(
                GameResultContract.Results.CONTENT_URI,
                null
            )
            throw AssertionError("Expected insert to reject writes")
        } catch (_: UnsupportedOperationException) {
        }
    }

    @Test
    fun deleteIsRejectedBecauseProviderIsReadOnly() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()

        try {
            provider.delete(
                ContentUris.withAppendedId(GameResultContract.Results.CONTENT_URI, 1L),
                null,
                null
            )
            throw AssertionError("Expected delete to reject writes")
        } catch (_: UnsupportedOperationException) {
        }
    }

    @Test
    fun itemUriStillQueriesInsertedResult() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()
        val id = mDb.gameResultDao().insert(
            com.bird.starryskysudoku.data.entity.GameResultEntity(
                mLevel = 3,
                mElapsedSeconds = 30,
                mRemainingSeconds = 570,
                mCompleted = 1,
                mCreatedAt = 1_800_000_000_003L,
                mUsername = "bob"
            )
        )

        provider.query(ContentUris.withAppendedId(GameResultContract.Results.CONTENT_URI, id), null, null, null, null)
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("bob", cursor.getString(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_USERNAME)))
            }
    }

    @Test
    fun updateIsRejectedBecauseProviderIsReadOnly() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()

        try {
            provider.update(
                GameResultContract.Results.CONTENT_URI,
                null,
                null,
                null
            )
            throw AssertionError("Expected update to reject writes")
        } catch (_: UnsupportedOperationException) {
        }
    }

    private fun setDatabaseInitializerInstance(db: AppDatabase?) {
        val field = DatabaseInitializer::class.java.getDeclaredField("sInstance")
        field.isAccessible = true
        field.set(DatabaseInitializer, db)
    }
}
