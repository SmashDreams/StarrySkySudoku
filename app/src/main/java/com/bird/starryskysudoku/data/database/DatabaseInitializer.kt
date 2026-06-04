package com.bird.starryskysudoku.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
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
            // 从这一版开始把地图进度按用户名拆分，游客历史也同步迁入新表。
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
            // 旧版本把游玩次数存成文本，这里整表重建并转换成整数列。
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

    internal val sMigration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 用户专属进度表已经完全接管地图数据，遗留公共 map 表可以安全移除。
            db.execSQL("DROP TABLE IF EXISTS map")
        }
    }

    @Volatile
    private var sInstance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        // 数据库实例按进程单例缓存，避免多页面并发打开多个 Room 实例。
        return sInstance ?: synchronized(this) {
            sInstance ?: buildDatabase(context).also { sInstance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
            .createFromAsset(DB_NAME)
            .addMigrations(sMigration1To2, sMigration2To3, sMigration3To4, sMigration4To5, sMigration5To6)
            .build()
    }

    /** 仅在 release 版本中对第 1、2 关做演示简化：将大部分空格填入正确答案，仅保留 2~3 个空。 */
    fun applyReleaseDemoPatches(db: AppDatabase) {
        if (com.bird.starryskysudoku.BuildConfig.DEBUG) return
        val sdb = db.openHelper.writableDatabase
        val done = sdb.query("SELECT value FROM problem WHERE pass_num=1 ORDER BY id LIMIT 1").use {
            it.moveToFirst() && it.getInt(0) != 0
        }
        if (done) return

        val levels = mapOf(
            1 to charArrayOf(
                '2','3','5','6','4','7','9','8','1',
                '7','4','9','8','1','5','2','3','6',
                '6','1','8','9','2','3','7','4','5',
                '3','5','1','2','7','6','8','9','4',
                '8','7','6','3','0','4','5','1','2',
                '4','9','2','1','5','8','6','7','3',
                '9','8','4','5','6','1','3','2','7',
                '1','6','3','7','8','2','4','5','9',
                '5','2','7','4','3','9','1','6','0'
            ),
            2 to charArrayOf(
                '0','2','1','8','4','5','6','0','9',
                '7','9','4','1','6','2','5','3','8',
                '5','6','8','3','7','9','1','4','2',
                '1','4','9','7','2','6','3','8','5',
                '6','3','2','4','0','8','7','9','1',
                '8','5','7','9','3','1','2','6','4',
                '2','7','6','5','8','4','9','1','3',
                '9','8','5','6','1','3','4','2','7',
                '4','1','3','2','9','7','8','5','6'
            )
        )

        sdb.beginTransaction()
        try {
            for ((passNum, cells) in levels) {
                val ids = sdb.query("SELECT id FROM problem WHERE pass_num=$passNum ORDER BY id").use { cursor ->
                    val list = mutableListOf<Long>()
                    while (cursor.moveToNext()) list.add(cursor.getLong(0))
                    list
                }
                for (i in cells.indices) {
                    sdb.execSQL("UPDATE problem SET value=${cells[i]} WHERE id=${ids[i]}")
                }
            }
            sdb.setTransactionSuccessful()
        } finally {
            sdb.endTransaction()
        }
    }
}
