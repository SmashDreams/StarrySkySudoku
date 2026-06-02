package com.bird.starryskysudoku.ui.play

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
    }
}
