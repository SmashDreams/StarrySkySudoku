package com.bird.starryskysudoku.ui.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PassListAdapterStructureTest {
    private val source = File("src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt").readText()

    @Test
    fun adapterDoesNotOwnDialogs() {
        assertFalse(source.contains("MyDialog"))
        assertFalse(source.contains("MyDialogManager"))
        assertFalse(source.contains("R.layout.dialog_passcheck"))
    }

    @Test
    fun adapterCancelsAnimationsWhenViewsAreRecycled() {
        assertTrue(source.contains("override fun onViewRecycled"))
        assertTrue(source.contains("cancelPendingAnimations"))
    }
}
