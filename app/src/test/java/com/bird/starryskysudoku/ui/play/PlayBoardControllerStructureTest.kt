package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayBoardControllerStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playActivityDelegatesBoardInitializationAndTouchHandling() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val controller = mSourceRoot.resolve("ui/play/PlayBoardController.kt")

        assertTrue(controller.isFile)
        assertTrue(activity.contains("PlayBoardController("))
        assertTrue(activity.contains("mBoardController.init()"))
        assertFalse(activity.contains("private fun initBoard"))
        assertFalse(activity.contains("setListener(object : BroadView.Listener"))
    }

    @Test
    fun boardControllerOwnsBoardObserverAndSelectionUi() {
        val controller = mSourceRoot.resolve("ui/play/PlayBoardController.kt").readText()

        assertTrue(controller.contains("mViewModel.initBoard"))
        assertTrue(controller.contains("mViewModel.mBoard.observe"))
        assertTrue(controller.contains("BroadView.Listener"))
        assertTrue(controller.contains("refreshCellActionAlpha"))
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
