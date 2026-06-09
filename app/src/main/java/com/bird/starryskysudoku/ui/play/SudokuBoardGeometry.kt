package com.bird.starryskysudoku.ui.play

object SudokuBoardGeometry {
    // 这些常量直接对应当前棋盘素材的留白和边框尺寸，供绘制与引导高亮共用。
    const val BOARD_SIZE = 9
    const val CELL_INSET = 28f
    const val BORDER_INSET = 24f
    const val CELL_SIZE_OFFSET = 54f

    data class BoardRect(
        val mLeft: Float,
        val mTop: Float,
        val mRight: Float,
        val mBottom: Float
    )

    // 通过视图宽度反推单格边长，保证不同屏幕尺寸下棋盘比例一致。
    fun cellSize(width: Float): Float = (width - CELL_SIZE_OFFSET) / BOARD_SIZE

    fun boardBorderRect(
        width: Float,
        left: Float = 0f,
        top: Float = 0f,
        padding: Float = 0f
    ): BoardRect {
        val boardSize = cellSize(width) * BOARD_SIZE
        // 外框从粗边框内侧开始计算，和棋盘实际绘制边界保持一致。
        return BoardRect(
            mLeft = left + BORDER_INSET - padding,
            mTop = top + BORDER_INSET - padding,
            mRight = left + BORDER_INSET + boardSize + padding,
            mBottom = top + BORDER_INSET + boardSize + padding
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
        // 右下边允许单独微调，便于引导蒙层与像素边框精确对齐。
        return BoardRect(
            mLeft = left + CELL_INSET + col * size - padding,
            mTop = top + CELL_INSET + row * size - padding,
            mRight = left + CELL_INSET + col * size - rightBottomAdjust + padding,
            mBottom = top + CELL_INSET + row * size - rightBottomAdjust + padding
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
            mLeft = left + BORDER_INSET + blockCol * blockSize - padding,
            mTop = top + BORDER_INSET + blockRow * blockSize - padding,
            mRight = left + BORDER_INSET + blockCol * blockSize + padding,
            mBottom = top + BORDER_INSET + blockRow * blockSize + padding
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
        // 区域矩形按网格线位置计算，适合做教程聚焦框或整块高亮。
        return BoardRect(
            mLeft = left + gridLineOffset(width, startCol) - padding,
            mTop = top + gridLineOffset(width, startRow) - padding,
            mRight = left + gridLineOffset(width, endCol) + padding,
            mBottom = top + gridLineOffset(width, endRow) + padding
        )
    }

    private fun gridLineOffset(width: Float, index: Int): Float {
        // 三宫边界使用更粗的外框内边距，普通格线使用单格内边距。
        val inset = if (index % 3 == 0) BORDER_INSET else CELL_INSET
        return inset + index * cellSize(width)
    }
}
