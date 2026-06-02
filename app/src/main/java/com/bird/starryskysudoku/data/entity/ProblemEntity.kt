package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "problem")
// 题库表按关卡存储一维数字序列，上层再映射成棋盘。
data class ProblemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val mId: Long = 0,
    @ColumnInfo(name = "pass_num") val mPassNum: Int,
    @ColumnInfo(name = "value") val mValue: Int
)
