package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayDialogControllerStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playActivityDelegatesDialogCreationToController() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val controller = mSourceRoot.resolve("ui/play/PlayDialogController.kt")

        assertTrue(controller.isFile)
        assertTrue(activity.contains("PlayDialogController("))
        assertTrue(mSourceRoot.resolve("ui/play/PlayNavigationController.kt").readText()
            .contains("mDialogController.showPauseDialog()"))
        assertTrue(activity.contains("mDialogController.hideAll()"))
        assertFalse(activity.contains("DialogPauseBinding.inflate"))
        assertFalse(activity.contains("DialogLoseBinding.inflate"))
        assertFalse(activity.contains("DialogWinBinding.inflate"))
    }

    @Test
    fun controllerOwnsDialogBindingsAndSettingsToggles() {
        val controller = mSourceRoot.resolve("ui/play/PlayDialogController.kt").readText()

        assertTrue(controller.contains("DialogPauseBinding.inflate"))
        assertTrue(controller.contains("DialogLoseBinding.inflate"))
        assertTrue(controller.contains("DialogWinBinding.inflate"))
        assertTrue(controller.contains("AppSettings.KEY_MUSIC"))
        assertTrue(controller.contains("AppSettings.KEY_AUDIO"))
    }

    @Test
    fun controllerStartsReplacementPlayActivityBeforeFinishingCurrentOne() {
        val controller = mSourceRoot.resolve("ui/play/PlayDialogController.kt").readText()
            .replace(Regex("\\s+"), " ")

        assertFalse(controller.contains("mActivity.finish() mActivity.startActivityWithTransition( PlayRoute.create"))
        assertFalse(controller.contains("mActivity.finish() val level = mGetLevel() if (level == mMaxLevel)"))
    }

    @Test
    fun replacementPlayNavigationDoesNotLetOldActivityStopNewCountdownServiceOnDestroy() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val controller = mSourceRoot.resolve("ui/play/PlayDialogController.kt").readText()

        assertTrue(activity.contains("mShouldStopCountdownOnDestroy"))
        assertTrue(activity.contains("if (mShouldStopCountdownOnDestroy)"))
        assertTrue(controller.contains("mPrepareForReplacementPlayActivity"))
        assertTrue(controller.contains("mPrepareForReplacementPlayActivity()"))
    }

    @Test
    fun nextLevelNavigationReturnsToMapBeforeShowingNextLevelEntry() {
        val controller = mSourceRoot.resolve("ui/play/PlayDialogController.kt").readText()

        assertTrue(controller.contains("MapRoute.createAfterWin"))
        assertTrue(controller.contains("completedLevel = level"))
        assertTrue(controller.contains("nextLevel = if (level < mMaxLevel) level + 1 else null"))
        assertTrue(controller.contains("createMapReturnIntent"))
        assertTrue(controller.contains("MapRoute.copyReturnAnchor"))
        assertFalse(controller.contains("startReplacementPlayActivity(level + 1)"))
    }

    @Test
    fun loseDialogReturnsToMapAndOnlyRetryRequestsLevelEntryDialog() {
        val controller = mSourceRoot.resolve("ui/play/PlayDialogController.kt").readText()
        val loseCloseBody = controller.substringAfter("loseDialogBinding.loseClose.setOnClickListener")
            .substringBefore("loseDialogBinding.loseRetry.setOnClickListener")
        val loseRetryBody = controller.substringAfter("loseDialogBinding.loseRetry.setOnClickListener")
            .substringBefore("private fun initWinDialog")

        assertTrue(loseCloseBody.contains("MapRoute.create(mActivity)"))
        assertFalse(loseCloseBody.contains("MapRoute.createAfterLose"))
        assertTrue(loseRetryBody.contains("MapRoute.createAfterLose(mActivity, mGetLevel())"))
        assertFalse(loseRetryBody.contains("startReplacementPlayActivity"))
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
