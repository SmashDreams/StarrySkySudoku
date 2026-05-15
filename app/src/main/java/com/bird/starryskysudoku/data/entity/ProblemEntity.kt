package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "problem")
data class ProblemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "pass_num") val passNum: Int,
    @ColumnInfo(name = "value") val value: Int
)
