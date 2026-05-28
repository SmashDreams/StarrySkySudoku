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
class DatabaseInitializerMigration4To5Test {
    private val mDbFile: File = RuntimeEnvironment.getApplication().getDatabasePath("migration-4-5-test.db")

    @After
    fun deleteDb() {
        mDbFile.delete()
    }

    @Test
    fun migration4To5ConvertsMapAndUserMapPlayTimeToInteger() {
        mDbFile.parentFile?.mkdirs()
        createVersion4Database()

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(mDbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(4, oldVersion)
                        assertEquals(5, newVersion)
                        DatabaseInitializer.sMigration4To5.migrate(db)
                    }
                })
                .build()
        )

        helper.writableDatabase.use { db ->
            db.query("PRAGMA table_info(map)").use { cursor ->
                val types = mutableMapOf<String, String>()
                while (cursor.moveToNext()) types[cursor.getString(1)] = cursor.getString(2)
                assertEquals("INTEGER", types["play_time"])
            }
            db.query("PRAGMA table_info(user_map)").use { cursor ->
                val types = mutableMapOf<String, String>()
                while (cursor.moveToNext()) types[cursor.getString(1)] = cursor.getString(2)
                assertEquals("INTEGER", types["play_time"])
            }
            db.query("SELECT play_time FROM map WHERE pass_num = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(7, cursor.getInt(0))
            }
            db.query("SELECT play_time FROM user_map WHERE username = 'alice' AND pass_num = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(4, cursor.getInt(0))
            }
        }
        helper.close()
    }

    private fun createVersion4Database() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(mDbFile.absolutePath)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
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
                            CREATE TABLE user_map (
                                username TEXT NOT NULL,
                                pass_num INTEGER NOT NULL,
                                status TEXT NOT NULL,
                                play_time TEXT NOT NULL,
                                PRIMARY KEY(username, pass_num)
                            )
                            """.trimIndent()
                        )
                        db.execSQL("INSERT INTO map(pass_num, status, play_time) VALUES(1, '待通关', '7')")
                        db.execSQL("INSERT INTO user_map(username, pass_num, status, play_time) VALUES('alice', 1, '已通关', '4')")
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()
        helper.close()
    }
}
