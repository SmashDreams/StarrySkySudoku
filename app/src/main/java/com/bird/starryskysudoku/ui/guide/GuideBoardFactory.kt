package com.bird.starryskysudoku.ui.guide

import com.bird.starryskysudoku.ui.play.BoardCell
import com.bird.starryskysudoku.ui.play.PlayViewModel

object GuideBoardFactory {
    private const val BOARD_SIZE = 9
    private const val DEMO_NUMBER = "7"
    private const val CENTER_INDEX = 4

    fun createBoard(values: List<Int>, step: GuideStep): Array<Array<BoardCell>> {
        val board = createBaseBoard(values)
        val selectedCell = findDemoEmptyCell(board)

        when (step) {
            GuideStep.WELCOME -> Unit
            GuideStep.RULE_UNIQUE,
            GuideStep.SELECT_CELL,
            GuideStep.TIMER -> highlightRelatedCells(board, selectedCell.first, selectedCell.second)
            GuideStep.ENTER_NUMBER -> {
                // 教学输入步骤固定把演示数字写入当前高亮空格。
                highlightRelatedCells(board, selectedCell.first, selectedCell.second)
                board[selectedCell.first][selectedCell.second].mValue = DEMO_NUMBER
            }
            GuideStep.GOOD_LUCK -> markDemoProgress(board)
        }

        return board
    }

    private fun createBaseBoard(values: List<Int>): Array<Array<BoardCell>> {
        return Array(BOARD_SIZE) { row ->
            Array(BOARD_SIZE) { col ->
                val value = values.getOrNull(row * BOARD_SIZE + col)?.takeIf { it in 0..9 } ?: 0
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

    private fun findDemoEmptyCell(board: Array<Array<BoardCell>>): Pair<Int, Int> {
        var bestCell: Pair<Int, Int>? = null
        var bestDistance = Int.MAX_VALUE
        // 优先找离棋盘中心最近的空格，让教程聚焦位置更稳定也更自然。
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (board[row][col].mValue == "0") {
                    val distance = kotlin.math.abs(row - CENTER_INDEX) + kotlin.math.abs(col - CENTER_INDEX)
                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestCell = row to col
                    }
                }
            }
        }
        return bestCell ?: (0 to 0)
    }

    private fun highlightRelatedCells(
        board: Array<Array<BoardCell>>,
        selectedRow: Int,
        selectedCol: Int
    ) {
        val selectedBlock = board[selectedRow][selectedCol].mBlock
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

    private fun markDemoProgress(board: Array<Array<BoardCell>>) {
        var filled = 0
        // 结束页只演示“棋盘逐步填满”的效果，不要求生成真实可解局面。
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                val cell = board[row][col]
                if (cell.mValue == "0" && filled < 6) {
                    cell.mValue = ((col + row) % BOARD_SIZE + 1).toString()
                    cell.mStatus = PlayViewModel.SELECT_ON
                    filled++
                }
            }
        }
    }
}
