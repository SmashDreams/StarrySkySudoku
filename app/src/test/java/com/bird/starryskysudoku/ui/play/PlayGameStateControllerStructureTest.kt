package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayGameStateControllerStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playActivityDelegatesTimerWinLoseAndWrongStateRendering() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val controller = mSourceRoot.resolve("ui/play/PlayGameStateController.kt")

        assertTrue(controller.isFile)
        assertTrue(activity.contains("PlayGameStateController("))
        assertTrue(activity.contains("mGameStateController.init()"))
        assertTrue(activity.contains("mGameStateController.clearCallbacks()"))
        assertFalse(activity.contains("private fun initTimer"))
        assertFalse(activity.contains("private fun playWinAnimation"))
        assertFalse(activity.contains("private fun failed"))
    }

    @Test
    fun gameStateControllerOwnsViewModelStateObserversAndAnimations() {
        val controller = mSourceRoot.resolve("ui/play/PlayGameStateController.kt").readText()

        assertTrue(controller.contains("mRemainingSeconds.observe"))
        assertTrue(controller.contains("mTimerFinished.observe"))
        assertTrue(controller.contains("mHasWon.observe"))
        assertTrue(controller.contains("mIsWrong.observe"))
        assertTrue(controller.contains("ObjectAnimator.ofInt"))
        assertTrue(controller.contains("showWinDialogWithStarAnimation"))
        assertTrue(controller.contains("showLoseDialog"))
    }

    private fun locateSourceRoot(): File {
        var dir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (true) {
            val sourceRoot = dir.resolve("src/main/java/com/bird/starryskysudoku")
            if (sourceRoot.isDirectory) return sourceRoot
            val appSourceRoot = dir.resolve("app/src/main/java/com/bird/starryskysudoku")
            if (appSourceRoot.isDirectory) return appSourceRoot
            dir = dir.parentFile ?: break
        }
        error("Unable to locate app source root")
    }
}
