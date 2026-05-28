package com.bird.starryskysudoku.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bird.starryskysudoku.data.dao.GameResultDao
import com.bird.starryskysudoku.data.dao.HistoryDao
import com.bird.starryskysudoku.data.dao.MapDao
import com.bird.starryskysudoku.data.dao.ProblemDao
import com.bird.starryskysudoku.data.dao.UserMapDao
import com.bird.starryskysudoku.data.entity.GameResultEntity
import com.bird.starryskysudoku.data.entity.HistoryEntity
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.entity.ProblemEntity
import com.bird.starryskysudoku.data.entity.UserMapEntity

@Database(
    entities = [ProblemEntity::class, MapEntity::class, UserMapEntity::class, HistoryEntity::class, GameResultEntity::class],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    /*
     * 题库、关卡、撤销历史和战绩共享都通过同一个数据库实例访问，
     * 确保内容提供器和游戏页面看到的数据保持一致。
     */
    abstract fun problemDao(): ProblemDao
    abstract fun mapDao(): MapDao
    abstract fun userMapDao(): UserMapDao
    abstract fun historyDao(): HistoryDao
    abstract fun gameResultDao(): GameResultDao
}
