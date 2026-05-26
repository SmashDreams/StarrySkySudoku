package com.bird.starryskysudoku.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "user_map", primaryKeys = ["username", "pass_num"])
data class UserMapEntity(
    @ColumnInfo(name = "username") val mUsername: String,
    @ColumnInfo(name = "pass_num") val mPassNum: Int,
    @ColumnInfo(name = "status") val mStatus: String,
    @ColumnInfo(name = "play_time") val mPlayTime: String
)
