package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map")
data class MapEntity(
    @PrimaryKey
    @ColumnInfo(name = "pass_num") val mPassNum: Int,
    @ColumnInfo(name = "status") val mStatus: String,
    @ColumnInfo(name = "play_time") val mPlayTime: String
)
