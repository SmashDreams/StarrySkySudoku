package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayInputControllerStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playActivityDelegatesNumberTagAndUndoInputs() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val controller = mSourceRoot.resolve("ui/play/PlayInputController.kt")

        assertTrue(controller.isFile)
        assertTrue(activity.contains("PlayInputController("))
        assertTrue(activity.contains("mInputController.init()"))
        assertFalse(activity.contains("private fun initTagButton"))
        assertFalse(activity.contains("private fun initInsertButtons"))
        assertFalse(activity.contains("private fun initRevokeButton"))
    }

    @Test
    fun inputControllerOwnsInputListenersAndCompletionCallback() {
        val controller = mSourceRoot.resolve("ui/play/PlayInputController.kt").readText()

        assertTrue(controller.contains("setOnTouchListener"))
        assertTrue(controller.contains("setOnClickListener"))
        assertTrue(controller.contains("insertNumber"))
        assertTrue(controller.contains("insertOrRemoveTag"))
        assertTrue(controller.contains("mOnPuzzleCompleted"))
    }

    @Test
    fun inputControllerDelegatesBoardAndTagMutationToViewModel() {
        val controller = mSourceRoot.resolve("ui/play/PlayInputController.kt").readText()

        assertFalse(controller.contains("mBoard.value!!"))
        assertFalse(controller.contains(".mValue ="))
        assertFalse(controller.contains(".mStatus ="))
        assertTrue(controller.contains("mViewModel.clearSelectionAfterEmptyUndo()"))
        assertTrue(controller.contains("mViewModel.restoreHistory("))
    }

    @Test
    fun playActivityHasDebugCompleteButtonForManualTesting() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val viewModel = mSourceRoot.resolve("ui/play/PlayViewModel.kt").readText()
        val layout = locateProjectRoot().resolve("app/src/main/res/layout/activity_play.xml").readText()

        assertTrue(layout.contains("@+id/play_debug_complete"))
        assertTrue(layout.contains("android:visibility=\"gone\""))
        assertTrue(activity.contains("initDebugCompleteButton()"))
        assertTrue(activity.contains("DEBUG_COMPLETE_TOGGLE_TAP_COUNT = 5"))
        assertTrue(activity.contains("mBinding.linearlayout3.setOnClickListener(toggleDebugComplete)"))
        assertTrue(activity.contains("mBinding.textview25.setOnClickListener(toggleDebugComplete)"))
        assertTrue(activity.contains("mPlayNum.setOnClickListener(toggleDebugComplete)"))
        assertTrue(activity.contains("mViewModel.updatePassStatus(mCurrentUsername, level, level + 1)"))
        assertTrue(activity.contains("mViewModel.markWonForDebug()"))
        assertTrue(viewModel.contains("fun markWonForDebug()"))
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

    private fun locateProjectRoot(): File {
        var dir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (true) {
            if (dir.resolve("app/src/main/res/layout/activity_play.xml").isFile) return dir
            if (dir.resolve("src/main/res/layout/activity_play.xml").isFile) return dir.parentFile ?: dir
            dir = dir.parentFile ?: break
        }
        error("Unable to locate project root")
    }
}
