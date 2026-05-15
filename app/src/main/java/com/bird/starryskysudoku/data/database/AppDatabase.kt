package com.bird.starryskysudoku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bird.starryskysudoku.data.dao.HistoryDao
import com.bird.starryskysudoku.data.dao.MapDao
import com.bird.starryskysudoku.data.dao.ProblemDao
import com.bird.starryskysudoku.data.entity.HistoryEntity
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.entity.ProblemEntity

@Database(
    entities = [ProblemEntity::class, MapEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun problemDao(): ProblemDao
    abstract fun mapDao(): MapDao
    abstract fun historyDao(): HistoryDao
}
