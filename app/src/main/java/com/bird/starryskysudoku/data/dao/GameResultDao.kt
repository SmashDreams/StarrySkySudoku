package com.bird.starryskysudoku.data.dao

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bird.starryskysudoku.data.entity.GameResultEntity

@Dao
interface GameResultDao {
    /*
     * 内容提供器的查询接口需要返回游标，因此这里让数据库访问对象直接返回游标。
     * 这样可以避免把实体列表再手动转换为游标，减少字段遗漏和类型错误。
     */
    @Query("SELECT _id, level, elapsed_seconds, remaining_seconds, completed, created_at, username FROM game_result ORDER BY created_at DESC")
    fun queryAll(): Cursor

    /*
     * 外部启动器按账号读取战绩时必须限定用户名，避免把其他用户的结果泄露到当前会话。
     */
    @Query("SELECT _id, level, elapsed_seconds, remaining_seconds, completed, created_at, username FROM game_result WHERE username = :username ORDER BY created_at DESC")
    fun queryByUsername(username: String): Cursor

    /*
     * 单条查询用于内容地址后追加编号的场景，例如外部应用查看某一次游戏战绩。
     */
    @Query("SELECT _id, level, elapsed_seconds, remaining_seconds, completed, created_at, username FROM game_result WHERE _id = :id")
    fun queryById(id: Long): Cursor

    /*
     * 插入操作由内容提供器统一入口调用，权限和字段校验都在进入这里前完成。
     */
    @Insert
    fun insert(result: GameResultEntity): Long

    /*
     * 删除仅支持按主键删除，避免外部调用方通过条件语句误删大量数据。
     */
    @Query("DELETE FROM game_result WHERE _id = :id")
    fun deleteById(id: Long): Int
}
