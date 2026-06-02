package com.bird.starryskysudoku.data.entity

// 地图页展示用的轻量模型，不直接暴露数据库实体的字段命名。
data class MapEntity(
    val mPassNum: Int,
    val mStatus: String,
    val mPlayTime: Int
)
