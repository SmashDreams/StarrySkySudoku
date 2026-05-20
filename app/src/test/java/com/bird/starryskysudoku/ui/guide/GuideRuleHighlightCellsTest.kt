package com.bird.starryskysudoku.ui.guide

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideRuleHighlightCellsTest {

    @Test
    fun `splits row and column around selected block to avoid overlapping dashed frames`() {
        val regions = GuideRuleHighlightCells.regionsFor(row = 4, col = 4)

        assertEquals(
            listOf(
                GuideRuleHighlightCells.CellRegion(startRow = 4, startCol = 0, rowSpan = 1, colSpan = 3),
                GuideRuleHighlightCells.CellRegion(startRow = 4, startCol = 6, rowSpan = 1, colSpan = 3),
                GuideRuleHighlightCells.CellRegion(startRow = 0, startCol = 4, rowSpan = 3, colSpan = 1),
                GuideRuleHighlightCells.CellRegion(startRow = 6, startCol = 4, rowSpan = 3, colSpan = 1),
                GuideRuleHighlightCells.CellRegion(startRow = 3, startCol = 3, rowSpan = 3, colSpan = 3)
            ),
            regions
        )
    }

    @Test
    fun `handles selected cells in edge blocks without zero sized segments`() {
        val regions = GuideRuleHighlightCells.regionsFor(row = 1, col = 7)

        assertEquals(
            listOf(
                GuideRuleHighlightCells.CellRegion(startRow = 1, startCol = 0, rowSpan = 1, colSpan = 6),
                GuideRuleHighlightCells.CellRegion(startRow = 3, startCol = 7, rowSpan = 6, colSpan = 1),
                GuideRuleHighlightCells.CellRegion(startRow = 0, startCol = 6, rowSpan = 3, colSpan = 3)
            ),
            regions
        )
    }
}
