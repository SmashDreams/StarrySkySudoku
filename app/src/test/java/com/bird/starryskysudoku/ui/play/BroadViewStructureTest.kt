package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BroadViewStructureTest {
    private val mSource = File("src/main/java/com/bird/starryskysudoku/ui/play/BroadView.kt").readText()

    @Test
    fun drawingCodeDoesNotSuppressAllocationWarnings() {
        assertFalse(mSource.contains("DrawAllocation"))
    }

    @Test
    fun drawingCodeReusesPaintAndRectObjects() {
        assertTrue(mSource.contains("private val mBitmapPaint"))
        assertTrue(mSource.contains("private val mCellRect"))
        assertFalse(mSource.contains("drawBitmap(mProblemLight, null, rect, Paint())"))
    }

    @Test
    fun broadViewUsesStandaloneBoardCellRenderModel() {
        val boardCell = File("src/main/java/com/bird/starryskysudoku/ui/play/BoardCell.kt")

        assertTrue(boardCell.isFile)
        assertTrue(mSource.contains("Array<Array<BoardCell>>"))
        assertFalse(mSource.contains("PlayViewModel.CellData"))
        assertFalse(boardCell.readText().contains("PlayViewModel"))
    }
}
