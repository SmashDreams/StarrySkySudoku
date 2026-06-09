package com.bird.starryskysudoku.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DatabaseInitializerMigration6To7Test {
    private val mDbFile: File = RuntimeEnvironment.getApplication().getDatabasePath("migration-6-7-test.db")

    @After
    fun deleteDb() {
        mDbFile.delete()
    }

    @Test
    fun migration6To7RemovesHistoryPassAndSessionColumns() {
        mDbFile.parentFile?.mkdirs()
        createVersion6Database()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(mDbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(6, oldVersion)
                        assertEquals(7, newVersion)
                        DatabaseInitializer.sMigration6To7.migrate(db)
                    }
                })
                .build()
        )

        helper.writableDatabase.use { db ->
            db.query("PRAGMA table_info(history)").use { cursor ->
                val columns = mutableSetOf<String>()
                while (cursor.moveToNext()) columns += cursor.getString(1)
                assertEquals(setOf("id", "row", "col", "type", "value"), columns)
                assertFalse(columns.contains("pass_num"))
                assertFalse(columns.contains("game_session"))
            }
            db.query("SELECT row, col, type, value FROM history ORDER BY id").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
                assertEquals(3, cursor.getInt(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals(9, cursor.getInt(3))
            }
        }
        helper.close()
    }

    private fun createVersion6Database() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(mDbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE history (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                row INTEGER NOT NULL,
                                col INTEGER NOT NULL,
                                type INTEGER NOT NULL,
                                value INTEGER NOT NULL,
                                pass_num INTEGER NOT NULL DEFAULT 0,
                                game_session TEXT NOT NULL DEFAULT ''
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            "INSERT INTO history(row, col, type, value, pass_num, game_session) VALUES(2, 3, 1, 9, 5, 'old-session')"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()
    }
}
