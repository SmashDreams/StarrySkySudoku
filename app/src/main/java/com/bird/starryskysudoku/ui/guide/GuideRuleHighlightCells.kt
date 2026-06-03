package com.bird.starryskysudoku.ui.guide

object GuideRuleHighlightCells {

    data class CellRegion(
        val mStartRow: Int,
        val mStartCol: Int,
        val mRowSpan: Int,
        val mColSpan: Int
    )

    fun regionsFor(row: Int, col: Int): List<CellRegion> {
        val blockStartRow = (row / 3) * 3
        val blockStartCol = (col / 3) * 3
        val blockEndRow = blockStartRow + 3
        val blockEndCol = blockStartCol + 3

        // 规则演示拆成“同行剩余区域 + 同列剩余区域 + 所在宫”，避免重复覆盖中心格。
        return buildList {
            addIfNotEmpty(row, 0, 1, blockStartCol)
            addIfNotEmpty(row, blockEndCol, 1, 9 - blockEndCol)
            addIfNotEmpty(0, col, blockStartRow, 1)
            addIfNotEmpty(blockEndRow, col, 9 - blockEndRow, 1)
            add(CellRegion(blockStartRow, blockStartCol, 3, 3))
        }
    }

    private fun MutableList<CellRegion>.addIfNotEmpty(
        startRow: Int,
        startCol: Int,
        rowSpan: Int,
        colSpan: Int
    ) {
        if (rowSpan > 0 && colSpan > 0) {
            add(CellRegion(startRow, startCol, rowSpan, colSpan))
        }
    }
}
