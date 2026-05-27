package com.bird.starryskysudoku.data.provider

import android.content.ContentUris
import androidx.room.Room
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    fun resolveResultsQueryAllowsUnfilteredCollectionQuery() {
        assertEquals(ResultQueryFilter.All, GameResultProvider.resolveResultsQueryFilter(null, null))
    }

    @Test
    fun queryRejectsSelectionArgsWithoutSelectionForCollectionUri() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()

        assertThrows(IllegalArgumentException::class.java) {
            provider.query(
                GameResultContract.Results.CONTENT_URI,
                null,
                null,
                arrayOf("alice"),
                null
            )
        }
    }

    @Test
    fun resolveResultsQueryTrimsUsernameForUsernameSelection() {
        assertEquals(
            ResultQueryFilter.Username("alice"),
            GameResultProvider.resolveResultsQueryFilter(
                GameResultContract.Results.selectionForUsername(),
                arrayOf(" alice ")
            )
        )
    }

    @Test
    fun resolveResultsQueryRejectsBlankUsernameSelectionArg() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultProvider.resolveResultsQueryFilter(
                GameResultContract.Results.selectionForUsername(),
                arrayOf("   ")
            )
        }
    }

    @Test
    fun resolveResultsQueryRejectsMissingUsernameSelectionArg() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultProvider.resolveResultsQueryFilter(
                GameResultContract.Results.selectionForUsername(),
                emptyArray()
            )
        }
    }

    @Test
    fun resolveResultsQueryRejectsMultipleUsernameSelectionArgs() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultProvider.resolveResultsQueryFilter(
                GameResultContract.Results.selectionForUsername(),
                arrayOf("alice", "bob")
            )
        }
    }

    @Test
    fun resolveResultsQueryRejectsUnsupportedSelection() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultProvider.resolveResultsQueryFilter("level=?", arrayOf("1"))
        }
    }

    @Test
    fun queryRejectsUsernameSelectionForItemUri() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()
        val itemUri = ContentUris.withAppendedId(GameResultContract.Results.CONTENT_URI, 1L)

        assertThrows(IllegalArgumentException::class.java) {
            provider.query(
                itemUri,
                null,
                GameResultContract.Results.selectionForUsername(),
                arrayOf("alice"),
                null
            )
        }
    }

    @Test
    fun deleteRejectsSelectionForItemUriAndKeepsResult() {
        val provider = Robolectric.buildContentProvider(GameResultProvider::class.java)
            .create()
            .get()
        val itemUri = provider.insert(
            GameResultContract.Results.CONTENT_URI,
            GameResultContract.Results.toContentValues(
                level = 1,
                elapsedSeconds = 10,
                remainingSeconds = 590,
                completed = false,
                createdAt = 1_800_000_000_002L,
                username = "alice"
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            provider.delete(
                itemUri,
                GameResultContract.Results.selectionForUsername(),
                arrayOf("alice")
            )
        }

        provider.query(itemUri, null, null, null, null).use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
    }

    private fun setDatabaseInitializerInstance(db: AppDatabase?) {
        val field = DatabaseInitializer::class.java.getDeclaredField("sInstance")
        field.isAccessible = true
        field.set(DatabaseInitializer, db)
    }
}
