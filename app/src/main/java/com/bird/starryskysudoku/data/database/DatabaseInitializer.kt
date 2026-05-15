package com.bird.starryskysudoku.data.database

import android.content.Context
import androidx.room.Room

object DatabaseInitializer {
    private const val DB_NAME = "sudoku.db"

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .createFromAsset(DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }
}
