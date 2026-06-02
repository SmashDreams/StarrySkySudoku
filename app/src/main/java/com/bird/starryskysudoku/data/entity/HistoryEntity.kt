package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
// 一条历史代表一次数字填写或候选数切换，用于撤销恢复。
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val mId: Long = 0,
    @ColumnInfo(name = "row") val mRow: Int,
    @ColumnInfo(name = "col") val mCol: Int,
    @ColumnInfo(name = "type") val mType: Int,
    @ColumnInfo(name = "value") val mValue: Int,
    @ColumnInfo(name = "pass_num", defaultValue = "0") val mPassNum: Int,
    @ColumnInfo(name = "game_session", defaultValue = "''") val mGameSession: String
)
