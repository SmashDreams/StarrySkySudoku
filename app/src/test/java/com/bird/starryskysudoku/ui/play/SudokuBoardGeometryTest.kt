package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertEquals
import org.junit.Test

class SudokuBoardGeometryTest {

    @Test
    fun `cell size matches BroadView drawing size`() {
        assertEquals(316f / 9f, SudokuBoardGeometry.cellSize(370f), 0.001f)
    }

    @Test
    fun `board border rect uses BroadView outer border inset`() {
        val rect = SudokuBoardGeometry.boardBorderRect(width = 370f, left = 10f, top = 20f, padding = 2f)

        assertEquals(32f, rect.left, 0.001f)
        assertEquals(42f, rect.top, 0.001f)
        assertEquals(352f, rect.right, 0.001f)
        assertEquals(362f, rect.bottom, 0.001f)
    }

    @Test
    fun `cell rect uses BroadView cell drawing inset and can tighten right bottom edges`() {
        val rect = SudokuBoardGeometry.cellRect(
            width = 370f,
            row = 4,
            col = 4,
            left = 10f,
            top = 20f,
            padding = 1f,
            rightBottomAdjust = 2f
        )
        val cellSize = 316f / 9f

        assertEquals(10f + 28f + 4 * cellSize - 1f, rect.left, 0.001f)
        assertEquals(20f + 28f + 4 * cellSize - 1f, rect.top, 0.001f)
        assertEquals(10f + 28f + 5 * cellSize - 2f + 1f, rect.right, 0.001f)
        assertEquals(20f + 28f + 5 * cellSize - 2f + 1f, rect.bottom, 0.001f)
    }

    @Test
    fun `block rect uses BroadView outer border inset without cell inset drift`() {
        val rect = SudokuBoardGeometry.blockRect(width = 370f, blockRow = 1, blockCol = 1, left = 10f, top = 20f, padding = 1f)
        val blockSize = 316f / 3f

        assertEquals(10f + 24f + blockSize - 1f, rect.left, 0.001f)
        assertEquals(20f + 24f + blockSize - 1f, rect.top, 0.001f)
        assertEquals(10f + 24f + 2 * blockSize + 1f, rect.right, 0.001f)
        assertEquals(20f + 24f + 2 * blockSize + 1f, rect.bottom, 0.001f)
    }

    @Test
    fun `board region rect uses matching grid line coordinates for rows and columns`() {
        val rect = SudokuBoardGeometry.boardRegionRect(
            width = 370f,
            startRow = 4,
            startCol = 0,
            rowSpan = 1,
            colSpan = 9,
            left = 10f,
            top = 20f,
            padding = 1f
        )
        val cellSize = 316f / 9f

        assertEquals(10f + 24f - 1f, rect.left, 0.001f)
        assertEquals(20f + 28f + 4 * cellSize - 1f, rect.top, 0.001f)
        assertEquals(10f + 24f + 9 * cellSize + 1f, rect.right, 0.001f)
        assertEquals(20f + 28f + 5 * cellSize + 1f, rect.bottom, 0.001f)
    }

    @Test
    fun `board region rect follows thin grid lines for non block boundaries`() {
        val rect = SudokuBoardGeometry.boardRegionRect(
            width = 370f,
            startRow = 4,
            startCol = 4,
            rowSpan = 1,
            colSpan = 1,
            left = 10f,
            top = 20f,
            padding = 0f
        )
        val cellSize = 316f / 9f

        assertEquals(10f + 28f + 4 * cellSize, rect.left, 0.001f)
        assertEquals(20f + 28f + 4 * cellSize, rect.top, 0.001f)
        assertEquals(10f + 28f + 5 * cellSize, rect.right, 0.001f)
        assertEquals(20f + 28f + 5 * cellSize, rect.bottom, 0.001f)
    }
}
