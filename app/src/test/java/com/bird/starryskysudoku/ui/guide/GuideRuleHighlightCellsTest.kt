package com.bird.starryskysudoku.ui.guide

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideRuleHighlightCellsTest {

    @Test
    fun `splits row and column around selected block to avoid overlapping dashed frames`() {
        val regions = GuideRuleHighlightCells.regionsFor(row = 4, col = 4)

        assertEquals(
            listOf(
                GuideRuleHighlightCells.CellRegion(mStartRow = 4, mStartCol = 0, mRowSpan = 1, mColSpan = 3),
                GuideRuleHighlightCells.CellRegion(mStartRow = 4, mStartCol = 6, mRowSpan = 1, mColSpan = 3),
                GuideRuleHighlightCells.CellRegion(mStartRow = 0, mStartCol = 4, mRowSpan = 3, mColSpan = 1),
                GuideRuleHighlightCells.CellRegion(mStartRow = 6, mStartCol = 4, mRowSpan = 3, mColSpan = 1),
                GuideRuleHighlightCells.CellRegion(mStartRow = 3, mStartCol = 3, mRowSpan = 3, mColSpan = 3)
            ),
            regions
        )
    }

    @Test
    fun `handles selected cells in edge blocks without zero sized segments`() {
        val regions = GuideRuleHighlightCells.regionsFor(row = 1, col = 7)

        assertEquals(
            listOf(
                GuideRuleHighlightCells.CellRegion(mStartRow = 1, mStartCol = 0, mRowSpan = 1, mColSpan = 6),
                GuideRuleHighlightCells.CellRegion(mStartRow = 3, mStartCol = 7, mRowSpan = 6, mColSpan = 1),
                GuideRuleHighlightCells.CellRegion(mStartRow = 0, mStartCol = 6, mRowSpan = 3, mColSpan = 3)
            ),
            regions
        )
    }
}
