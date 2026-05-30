package com.bird.starryskysudoku.ui.play

object PlayBoardRules {
    data class InsertResult(
        val mWasCleared: Boolean,
        val mIsWrong: Boolean,
        val mIsComplete: Boolean,
        val mNewLastValue: String
    )

    fun createBoard(values: List<Int>): Array<Array<PlayViewModel.CellData>>? {
        if (values.size != BOARD_CELL_COUNT || values.any { it !in 0..9 }) return null
        return Array(BOARD_SIZE) { row ->
            Array(BOARD_SIZE) { col ->
                val index = row * BOARD_SIZE + col
                val value = values[index]
                PlayViewModel.CellData(
                    mRow = row,
                    mCol = col,
                    mValue = value.toString(),
                    mBlock = blockOf(row, col),
                    mType = if (value == 0) PlayViewModel.EMPTY else PlayViewModel.PROBLEM
                )
            }
        }
    }

    fun selectCell(
        board: Array<Array<PlayViewModel.CellData>>,
        row: Int,
        col: Int
    ): String {
        val number = board[row][col].mValue
        board[row][col].mStatus = PlayViewModel.BE_SELECTED
        if (number == EMPTY_VALUE) {
            highlightEmptyCellPeers(board, row, col, board[row][col].mBlock)
        } else {
            highlightMatchingNumbers(board, row, col, number)
        }
        return number
    }

    fun insertNumber(
        board: Array<Array<PlayViewModel.CellData>>,
        row: Int,
        col: Int,
        number: String,
        lastValue: String = EMPTY_VALUE
    ): InsertResult {
        val cell = board[row][col]
        if (cell.mValue == number) {
            cell.mValue = EMPTY_VALUE
            cell.mStatus = PlayViewModel.BE_SELECTED
            highlightEmptyCellPeers(board, row, col, cell.mBlock)
            return InsertResult(
                mWasCleared = true,
                mIsWrong = false,
                mIsComplete = false,
                mNewLastValue = EMPTY_VALUE
            )
        }

        cell.mValue = number
        cell.mStatus = PlayViewModel.SELECT_ON
        var wrongOrEmptyCount = 0
        for (i in 0 until BOARD_SIZE) {
            for (j in 0 until BOARD_SIZE) {
                if (i != row || j != col) {
                    refreshPeerStatus(board, row, col, number, cell.mBlock, i, j)
                }
                if (board[i][j].mValue == EMPTY_VALUE || board[i][j].mStatus == PlayViewModel.WRONG) {
                    wrongOrEmptyCount++
                }
            }
        }
        return InsertResult(
            mWasCleared = false,
            mIsWrong = cell.mStatus == PlayViewModel.WRONG,
            mIsComplete = wrongOrEmptyCount == 0 && cell.mStatus != PlayViewModel.WRONG,
            mNewLastValue = if (cell.mStatus == PlayViewModel.WRONG) lastValue else cell.mValue
        )
    }

    fun revertWrongInput(
        board: Array<Array<PlayViewModel.CellData>>,
        row: Int,
        col: Int,
        lastValue: String
    ) {
        board[row][col].mValue = lastValue
        board[row][col].mStatus = PlayViewModel.BE_SELECTED
        if (lastValue == EMPTY_VALUE) {
            highlightEmptyCellPeers(board, row, col, board[row][col].mBlock)
        } else {
            highlightMatchingNumbers(board, row, col, lastValue)
        }
    }

    private fun refreshPeerStatus(
        board: Array<Array<PlayViewModel.CellData>>,
        row: Int,
        col: Int,
        number: String,
        block: Int,
        peerRow: Int,
        peerCol: Int
    ) {
        val peer = board[peerRow][peerCol]
        peer.mStatus = when {
            peer.mValue == number && (peer.mRow == row || peer.mCol == col || peer.mBlock == block) -> {
                board[row][col].mStatus = PlayViewModel.WRONG
                PlayViewModel.WRONG
            }
            peer.mValue == number -> PlayViewModel.SELECT_ON
            else -> PlayViewModel.SELECT_NONE
        }
    }

    private fun highlightMatchingNumbers(
        board: Array<Array<PlayViewModel.CellData>>,
        currentRow: Int,
        currentCol: Int,
        number: String
    ) {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (row != currentRow || col != currentCol) {
                    board[row][col].mStatus = if (board[row][col].mValue == number) {
                        PlayViewModel.SELECT_ON
                    } else {
                        PlayViewModel.SELECT_NONE
                    }
                }
            }
        }
    }

    private fun highlightEmptyCellPeers(
        board: Array<Array<PlayViewModel.CellData>>,
        currentRow: Int,
        currentCol: Int,
        block: Int
    ) {
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                if (row != currentRow || col != currentCol) {
                    val cell = board[row][col]
                    cell.mStatus = if (cell.mRow == currentRow || cell.mCol == currentCol || cell.mBlock == block) {
                        PlayViewModel.SELECT_ON
                    } else {
                        PlayViewModel.SELECT_NONE
                    }
                }
            }
        }
    }

    private fun blockOf(row: Int, col: Int): Int {
        return (row / BLOCK_SIZE) * BLOCK_SIZE + (col / BLOCK_SIZE) + 1
    }

    private const val BOARD_SIZE = 9
    private const val BOARD_CELL_COUNT = BOARD_SIZE * BOARD_SIZE
    private const val BLOCK_SIZE = 3
    private const val EMPTY_VALUE = "0"
}
