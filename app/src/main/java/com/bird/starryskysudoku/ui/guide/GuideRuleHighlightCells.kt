package com.bird.starryskysudoku.ui.guide

object GuideRuleHighlightCells {

    data class CellRegion(
        val startRow: Int,
        val startCol: Int,
        val rowSpan: Int,
        val colSpan: Int
    )

    fun regionsFor(row: Int, col: Int): List<CellRegion> {
        val blockStartRow = (row / 3) * 3
        val blockStartCol = (col / 3) * 3
        val blockEndRow = blockStartRow + 3
        val blockEndCol = blockStartCol + 3

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
