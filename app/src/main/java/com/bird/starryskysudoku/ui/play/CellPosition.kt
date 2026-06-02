package com.bird.starryskysudoku.ui.play

// 当前棋盘焦点位置，统一供输入、撤销和高亮逻辑共享。
data class CellPosition(
    val mRow: Int,
    val mCol: Int,
    val mBlock: Int
)
