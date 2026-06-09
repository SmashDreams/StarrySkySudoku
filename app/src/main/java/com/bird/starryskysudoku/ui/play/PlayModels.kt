package com.bird.starryskysudoku.ui.play

// 当前棋盘焦点位置，统一供输入、撤销和高亮逻辑共享。
data class CellPosition(
    val mRow: Int,
    val mCol: Int,
    val mBlock: Int
)

// 用固定长度数组保存一到九的候选数，便于和数字键索引直接对应。
class TagData {
    val mTags: Array<String> = Array(9) { "0" }

    fun setTag(number: String) {
        val idx = number.toIntOrNull() ?: return
        if (idx in 1..9) mTags[idx - 1] = number
    }

    fun deleteTag(number: String) {
        val idx = number.toIntOrNull() ?: return
        if (idx in 1..9) mTags[idx - 1] = "0"
    }

    fun haveTag(number: String): Boolean {
        val idx = number.toIntOrNull() ?: return false
        return idx in 1..9 && mTags[idx - 1] != "0"
    }
}

// 棋盘单元格数据及其高亮和类型标记。
data class BoardCell(
    var mRow: Int,
    var mCol: Int,
    var mValue: String,
    var mBlock: Int,
    var mStatus: Int = SELECT_NONE,
    var mType: Int = EMPTY
) {
    companion object {
        // 棋盘高亮状态：未选中 / 当前选中 / 与当前选中值相关 / 错误输入。
        const val SELECT_NONE = 0
        const val SELECT_ON = 1
        const val BE_SELECTED = -1
        const val WRONG = 2

        // 棋盘单元格类型：题面给定数字或玩家可填写空格。
        const val PROBLEM = 1
        const val EMPTY = 0

        // 按钮半禁用态的透明度值。
        const val DIM_ALPHA = 0.55f
    }
}
