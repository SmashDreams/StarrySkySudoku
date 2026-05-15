package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "row") val row: Int,
    @ColumnInfo(name = "col") val col: Int,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "value") val value: Int,
    @ColumnInfo(name = "pass_num", defaultValue = "0") val passNum: Int,
    @ColumnInfo(name = "game_session", defaultValue = "''") val gameSession: String
)
