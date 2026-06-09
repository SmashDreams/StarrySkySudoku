package com.bird.starryskysudoku.ui.guide

import com.bird.starryskysudoku.ui.play.BoardCell
import com.bird.starryskysudoku.ui.play.PlayViewModel

object GuideBoardFactory {
    private const val BOARD_SIZE = 9
    private const val DEMO_NUMBER = "7"
    private const val CENTER_ROW = 4
    private const val CENTER_COL = 4

    fun createBoard(values: List<Int>, step: GuideStep): Array<Array<BoardCell>> {
        // 教学棋盘始终从真实题面复制一份，再按当前步骤叠加演示高亮和示例数字。
        val board = createBaseBoard(values)
        val selectedRow = CENTER_ROW
        val selectedCol = CENTER_COL

        when (step) {
            GuideStep.WELCOME -> Unit
            GuideStep.RULE_UNIQUE,
            GuideStep.SELECT_CELL,
            GuideStep.TIMER -> highlightRelatedCells(board, selectedRow, selectedCol)
            GuideStep.ENTER_NUMBER -> {
                highlightRelatedCells(board, selectedRow, selectedCol)
                board[selectedRow][selectedCol].mValue = DEMO_NUMBER
            }
            GuideStep.GOOD_LUCK -> Unit
        }

        return board
    }

    private fun createBaseBoard(values: List<Int>): Array<Array<BoardCell>> {
        return Array(BOARD_SIZE) { row ->
            Array(BOARD_SIZE) { col ->
                val value = values[row * BOARD_SIZE + col]
                BoardCell(
                    mRow = row,
                    mCol = col,
                    mValue = value.toString(),
                    mBlock = (row / 3) * 3 + (col / 3) + 1,
                    mType = if (value == 0) PlayViewModel.EMPTY else PlayViewModel.PROBLEM
                )
            }
        }
    }

    private fun highlightRelatedCells(
        board: Array<Array<BoardCell>>,
        selectedRow: Int,
        selectedCol: Int
    ) {
        val selectedBlock = board[selectedRow][selectedCol].mBlock
        // 教学高亮复用正式棋盘的“同行、同列、同宫”规则，保证引导和实战一致。
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val cell = board[row][col]
                cell.mStatus = when {
                    row == selectedRow && col == selectedCol -> PlayViewModel.BE_SELECTED
                    row == selectedRow || col == selectedCol || cell.mBlock == selectedBlock -> PlayViewModel.SELECT_ON
                    else -> PlayViewModel.SELECT_NONE
                }
            }
        }
    }
}
