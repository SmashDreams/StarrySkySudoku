package com.bird.starryskysudoku.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DialogAndAdapterBindingStructureTest {
    @Test
    fun passListAdapterUsesItemViewBinding() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt").readText()

        assertTrue(source.contains("PassItemBinding"))
        assertTrue(source.contains("PassFirstItemBinding"))
        assertFalse(source.contains("itemView.findViewById"))
    }

    @Test
    fun mapActivityUsesDialogBindingsForSettingsAndPassCheck() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt").readText()

        assertTrue(source.contains("DialogPasscheckBinding"))
        assertTrue(source.contains("DialogSettingsBinding"))
        assertFalse(source.contains("mSettingsDialog.findViewById"))
        assertFalse(source.contains("findViewById<TextView>(R.id.passcheck"))
        assertFalse(source.contains("findViewById<ImageView>(R.id.passcheck"))
    }

    @Test
    fun playActivityUsesDialogBindingsForPauseWinAndLose() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/play/PlayDialogController.kt").readText()

        assertTrue(source.contains("DialogPauseBinding"))
        assertTrue(source.contains("DialogWinBinding"))
        assertTrue(source.contains("DialogLoseBinding"))
        assertFalse(source.contains("mPauseDialog.findViewById"))
        assertFalse(source.contains("mWinDialog.findViewById"))
        assertFalse(source.contains("mLoseDialog.findViewById"))
    }
}
