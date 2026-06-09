package com.bird.starryskysudoku.ui.play

object PlayBoardRules {
    /*
     * 插入数字后一次性返回本次操作的关键信息，
     * 让视图模型不必重复遍历棋盘推导结果。
     */
    data class InsertResult(
        val mWasCleared: Boolean,
        val mIsWrong: Boolean,
        val mIsComplete: Boolean,
        val mNewLastValue: String
    )

    fun createBoard(values: List<Int>): Array<Array<BoardCell>>? {
        if (values.size != BOARD_CELL_COUNT || values.any { it !in 0..9 }) return null
        return Array(BOARD_SIZE) { row -> Array(BOARD_SIZE) { col ->
                // 题库是一维数组，进入界面前转换成按行列访问的二维棋盘。
                val index = row * BOARD_SIZE + col
                val value = values[index]
                BoardCell(
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
        board: Array<Array<BoardCell>>,
        row: Int,
        col: Int
    ): String {
        // 选中有数字的格子时高亮相同数字；选中空格时高亮同行同列同宫。
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
        board: Array<Array<BoardCell>>,
        row: Int,
        col: Int,
        number: String,
        lastValue: String = EMPTY_VALUE
    ): InsertResult {
        val cell = board[row][col]
        if (cell.mValue == number) {
            // 再次点击同一个数字等价于清空该格，并恢复空格高亮效果。
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
                // 先刷新当前输入与其相关格子的状态，再统计是否已完成整盘。
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
        board: Array<Array<BoardCell>>,
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
        board: Array<Array<BoardCell>>,
        row: Int,
        col: Int,
        number: String,
        block: Int,
        peerRow: Int,
        peerCol: Int
    ) {
        val peer = board[peerRow][peerCol]
        val isConflict = peer.mValue == number &&
            (peer.mRow == row || peer.mCol == col || peer.mBlock == block)
        if (isConflict) {
            // 当前输入格和冲突格同时标记为错误，便于棋盘上直观显示冲突。
            board[row][col].mStatus = PlayViewModel.WRONG
        }
        peer.mStatus = when {
            isConflict -> PlayViewModel.WRONG
            peer.mValue == number -> PlayViewModel.SELECT_ON
            else -> PlayViewModel.SELECT_NONE
        }
    }

    private fun highlightMatchingNumbers(
        board: Array<Array<BoardCell>>,
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
        board: Array<Array<BoardCell>>,
        currentRow: Int,
        currentCol: Int,
        block: Int
    ) {
        // 空格选中态需要提示玩家本次填写会影响哪些格子。
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
        // 宫号按 1..9 返回，保持与旧界面状态字段的约定一致。
        return (row / BLOCK_SIZE) * BLOCK_SIZE + (col / BLOCK_SIZE) + 1
    }

    private const val BOARD_SIZE = 9
    private const val BOARD_CELL_COUNT = 81
    private const val BLOCK_SIZE = 3
    private const val EMPTY_VALUE = "0"
}
