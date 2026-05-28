package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BroadViewStructureTest {
    private val source = File("src/main/java/com/bird/starryskysudoku/ui/play/BroadView.kt").readText()

    @Test
    fun drawingCodeDoesNotSuppressAllocationWarnings() {
        assertFalse(source.contains("DrawAllocation"))
    }

    @Test
    fun drawingCodeReusesPaintAndRectObjects() {
        assertTrue(source.contains("private val mBitmapPaint"))
        assertTrue(source.contains("private val mCellRect"))
        assertFalse(source.contains("drawBitmap(mProblemLight, null, rect, Paint())"))
    }
}
