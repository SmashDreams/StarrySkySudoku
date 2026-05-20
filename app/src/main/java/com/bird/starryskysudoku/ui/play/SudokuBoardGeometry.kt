package com.bird.starryskysudoku.ui.play

object SudokuBoardGeometry {
    const val BOARD_SIZE = 9
    const val CELL_INSET = 28f
    const val BORDER_INSET = 24f
    const val CELL_SIZE_OFFSET = 54f

    data class BoardRect(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )

    fun cellSize(width: Float): Float = (width - CELL_SIZE_OFFSET) / BOARD_SIZE

    fun boardBorderRect(
        width: Float,
        left: Float = 0f,
        top: Float = 0f,
        padding: Float = 0f
    ): BoardRect {
        val boardSize = cellSize(width) * BOARD_SIZE
        return BoardRect(
            left = left + BORDER_INSET - padding,
            top = top + BORDER_INSET - padding,
            right = left + BORDER_INSET + boardSize + padding,
            bottom = top + BORDER_INSET + boardSize + padding
        )
    }

    fun cellRect(
        width: Float,
        row: Int,
        col: Int,
        left: Float = 0f,
        top: Float = 0f,
        padding: Float = 0f,
        rightBottomAdjust: Float = 0f
    ): BoardRect {
        val size = cellSize(width)
        return BoardRect(
            left = left + CELL_INSET + col * size - padding,
            top = top + CELL_INSET + row * size - padding,
            right = left + CELL_INSET + (col + 1) * size - rightBottomAdjust + padding,
            bottom = top + CELL_INSET + (row + 1) * size - rightBottomAdjust + padding
        )
    }

    fun blockRect(
        width: Float,
        blockRow: Int,
        blockCol: Int,
        left: Float = 0f,
        top: Float = 0f,
        padding: Float = 0f
    ): BoardRect {
        val blockSize = cellSize(width) * 3
        return BoardRect(
            left = left + BORDER_INSET + blockCol * blockSize - padding,
            top = top + BORDER_INSET + blockRow * blockSize - padding,
            right = left + BORDER_INSET + (blockCol + 1) * blockSize + padding,
            bottom = top + BORDER_INSET + (blockRow + 1) * blockSize + padding
        )
    }

    fun boardRegionRect(
        width: Float,
        startRow: Int,
        startCol: Int,
        rowSpan: Int,
        colSpan: Int,
        left: Float = 0f,
        top: Float = 0f,
        padding: Float = 0f
    ): BoardRect {
        val endRow = startRow + rowSpan
        val endCol = startCol + colSpan
        return BoardRect(
            left = left + gridLineOffset(width, startCol) - padding,
            top = top + gridLineOffset(width, startRow) - padding,
            right = left + gridLineOffset(width, endCol) + padding,
            bottom = top + gridLineOffset(width, endRow) + padding
        )
    }

    private fun gridLineOffset(width: Float, index: Int): Float {
        val inset = if (index % 3 == 0) BORDER_INSET else CELL_INSET
        return inset + index * cellSize(width)
    }
}
