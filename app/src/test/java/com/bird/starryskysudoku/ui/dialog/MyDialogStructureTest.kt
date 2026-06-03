package com.bird.starryskysudoku.ui.dialog

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MyDialogStructureTest {
    @Test
    fun dialogLocksClickableChildrenUntilEnterAnimationFinishes() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/dialog/MyDialog.kt").readText()

        assertTrue(source.contains("DEFAULT_ENTER_ANIMATION_MILLIS = 800L"))
        assertTrue(source.contains("override fun show()"))
        assertTrue(source.contains("lockInteractionsUntilEnterAnimationEnds()"))
        assertTrue(source.contains("collectClickableViews"))
        assertTrue(source.contains("view !is ViewGroup"))
        assertTrue(source.contains("view.isEnabled = false"))
        assertTrue(source.contains("handler.postDelayed(runnable, interactionLockDurationMillis)"))
        assertTrue(source.contains("if (interactionLocked) return"))
        assertTrue(source.contains("dismissImmediately()"))
        assertTrue(source.contains("windowAttributes.windowAnimations = 0"))
    }
}
