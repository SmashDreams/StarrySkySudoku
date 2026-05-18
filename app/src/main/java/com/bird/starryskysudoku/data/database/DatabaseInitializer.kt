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

    @Volatile
    private var sInstance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return sInstance ?: synchronized(this) {
            sInstance ?: buildDatabase(context).also { sInstance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
            .createFromAsset(DB_NAME)
            .addMigrations(sMigration1To2)
            .build()
    }
}
