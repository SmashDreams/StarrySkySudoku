package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "problem")
data class ProblemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val mId: Long = 0,
    @ColumnInfo(name = "pass_num") val mPassNum: Int,
    @ColumnInfo(name = "value") val mValue: Int
)
