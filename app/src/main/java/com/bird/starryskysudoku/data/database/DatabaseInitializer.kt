package com.bird.starryskysudoku.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseInitializer {
    private const val DB_NAME = "sudoku.db"

    private val sMigration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE history ADD COLUMN pass_num INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE history ADD COLUMN game_session TEXT NOT NULL DEFAULT ''")
        }
    }

    private val sMigration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            /*
             * 第三个数据库版本新增公开战绩表。
             * 主键列名遵循内容提供器游标的通用约定。
             */
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS game_result (
                    _id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    level INTEGER NOT NULL,
                    elapsed_seconds INTEGER NOT NULL,
                    remaining_seconds INTEGER NOT NULL,
                    completed INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    internal val sMigration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_map (
                    username TEXT NOT NULL,
                    pass_num INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    play_time TEXT NOT NULL,
                    PRIMARY KEY(username, pass_num)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO user_map(username, pass_num, status, play_time)
                SELECT 'guest', pass_num, status, play_time FROM map
                """.trimIndent()
            )
            db.execSQL("ALTER TABLE game_result ADD COLUMN username TEXT NOT NULL DEFAULT 'guest'")
        }
    }

    internal val sMigration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE map_v5 (
                    pass_num INTEGER NOT NULL PRIMARY KEY,
                    status TEXT NOT NULL,
                    play_time INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO map_v5(pass_num, status, play_time)
                SELECT pass_num, status, CAST(play_time AS INTEGER) FROM map
                """.trimIndent()
            )
            db.execSQL("DROP TABLE map")
            db.execSQL("ALTER TABLE map_v5 RENAME TO map")

            db.execSQL(
                """
                CREATE TABLE user_map_v5 (
                    username TEXT NOT NULL,
                    pass_num INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    play_time INTEGER NOT NULL,
                    PRIMARY KEY(username, pass_num)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO user_map_v5(username, pass_num, status, play_time)
                SELECT username, pass_num, status, CAST(play_time AS INTEGER) FROM user_map
                """.trimIndent()
            )
            db.execSQL("DROP TABLE user_map")
            db.execSQL("ALTER TABLE user_map_v5 RENAME TO user_map")
        }
    }

    @Volatile
    private var sInstance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return sInstance ?: synchronized(this) {
            sInstance ?: buildDatabase(context).also { sInstance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        /*
         * 初始题库仍从内置资源创建，后续版本只通过迁移补齐新增表结构。
         * 这样既保留已有题库数据，也能让旧用户平滑升级。
         */
        return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
            .createFromAsset(DB_NAME)
            .addMigrations(sMigration1To2, sMigration2To3, sMigration3To4, sMigration4To5)
            .build()
    }
}
