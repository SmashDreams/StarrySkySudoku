package com.bird.starryskysudoku.ui.map

import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.repository.PassStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PassListAdapterStructureTest {
    private val mSource = File("src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt").readText()

    @Test
    fun adapterDoesNotOwnDialogs() {
        assertFalse(mSource.contains("MyDialog"))
        assertFalse(mSource.contains("MyDialogManager"))
        assertFalse(mSource.contains("R.layout.dialog_passcheck"))
    }

    @Test
    fun adapterCancelsAnimationsWhenViewsAreRecycled() {
        assertTrue(mSource.contains("override fun onViewRecycled"))
        assertTrue(mSource.contains("cancelPendingAnimations"))
    }

    @Test
    fun adapterUsesCanvasOverlayInsteadOfHardCodedPathImages() {
        val layout = File("src/main/res/layout/pass_item.xml").readText()
        val overlay = File("src/main/java/com/bird/starryskysudoku/ui/map/MapPathOverlayView.kt").readText()

        assertTrue(layout.contains("MapPathOverlayView"))
        assertTrue(layout.contains("android:layout_height=\"340dp\""))
        assertTrue(layout.contains("android:layout_marginTop=\"84dp\""))
        assertTrue(layout.contains("android:layout_marginTop=\"168dp\""))
        assertTrue(layout.contains("android:layout_marginTop=\"252dp\""))
        assertTrue(mSource.contains("binding.pathOverlay"))
        assertTrue(mSource.contains("mPathOverlay?.bind"))
        assertTrue(mSource.contains("getPositionForLevel"))
        assertTrue(mSource.contains("getTopOffsetDpForLevel"))
        assertTrue(mSource.contains("LEVEL_VERTICAL_STEP_DP = 84"))
        assertTrue(mSource.contains("previousRowBelow"))
        assertTrue(mSource.contains("hasNextRowAbove"))
        assertFalse(mSource.contains("mLines"))
        assertFalse(layout.contains("@+id/line_"))
        assertTrue(overlay.contains("canvas.drawLine"))
        assertTrue(overlay.contains("DashPathEffect"))
        assertTrue(overlay.contains("drawIncomingConnector"))
        assertTrue(overlay.contains("drawOutgoingConnector"))
    }

    @Test
    fun adapterKeepsThirdLevelCompletionOffsetWhenReturningFromNextLevel() {
        val adapter = PassListAdapter(
            listOf(
                arrayOf(
                    MapEntity(1, PassStatus.COMPLETED, 1),
                    MapEntity(2, PassStatus.COMPLETED, 1),
                    MapEntity(3, PassStatus.COMPLETED, 1),
                    MapEntity(4, PassStatus.TODO, 0)
                )
            ),
            0
        )

        assertEquals(102, adapter.getCurrentProgressOffsetDp())
    }

    @Test
    fun adapterExposesDifferentTopOffsetsForLevelsInTheSameRow() {
        val adapter = PassListAdapter(emptyList(), 0)

        assertEquals(84, adapter.getTopOffsetDpForLevel(3))
        assertEquals(0, adapter.getTopOffsetDpForLevel(4))
        assertEquals(252, adapter.getTopOffsetDpForLevel(5))
        assertEquals(0, adapter.getTopOffsetDpForLevel(8))
    }
}
