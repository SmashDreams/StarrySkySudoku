package com.bird.starryskysudoku.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DatabaseInitializerMigrationTest {
    private val mDbFile: File = RuntimeEnvironment.getApplication().getDatabasePath("migration-3-4-test.db")

    @After
    fun deleteDb() {
        mDbFile.delete()
    }

    @Test
    fun migration3To4CreatesUserMapSeedsGuestRowsAndDefaultsGameResultUsername() {
        mDbFile.parentFile?.mkdirs()
        createVersion3Database()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(mDbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(3, oldVersion)
                        assertEquals(4, newVersion)
                        DatabaseInitializer.sMigration3To4.migrate(db)
                    }
                })
                .build()
        )

        helper.writableDatabase.use { db ->
            db.query("SELECT status, play_time FROM user_map WHERE username = 'guest' AND pass_num = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("待通关", cursor.getString(0))
                assertEquals("7", cursor.getString(1))
            }
            db.query("SELECT username FROM game_result WHERE _id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("guest", cursor.getString(0))
            }
        }
        helper.close()
    }

    private fun createVersion3Database() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(mDbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE map (
                                pass_num INTEGER NOT NULL PRIMARY KEY,
                                status TEXT NOT NULL,
                                play_time TEXT NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE game_result (
                                _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                level INTEGER NOT NULL,
                                elapsed_seconds INTEGER NOT NULL,
                                remaining_seconds INTEGER NOT NULL,
                                completed INTEGER NOT NULL,
                                created_at INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL("INSERT INTO map(pass_num, status, play_time) VALUES(1, '待通关', '7')")
                        db.execSQL("INSERT INTO game_result(level, elapsed_seconds, remaining_seconds, completed, created_at) VALUES(1, 10, 590, 0, 1800000000000)")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()
    }
}
