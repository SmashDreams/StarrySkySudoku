package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayNavigationControllerStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playActivityDelegatesPauseBackAndResumeNavigation() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val controller = mSourceRoot.resolve("ui/play/PlayNavigationController.kt")

        assertTrue(controller.isFile)
        assertTrue(activity.contains("PlayNavigationController("))
        assertTrue(activity.contains("mNavigationController.init()"))
        assertTrue(activity.contains("mNavigationController.onPause()"))
        assertTrue(activity.contains("mNavigationController.onResume()"))
        assertFalse(activity.contains("private fun initPauseButton"))
        assertFalse(activity.contains("private fun initBackHandler"))
        assertFalse(activity.contains("OnBackPressedCallback"))
    }

    @Test
    fun navigationControllerOwnsPauseDialogAndBackHandlerBehavior() {
        val controller = mSourceRoot.resolve("ui/play/PlayNavigationController.kt").readText()

        assertTrue(controller.contains("OnBackPressedCallback"))
        assertTrue(controller.contains("setOnClickListener"))
        assertTrue(controller.contains("showPauseDialog"))
        assertTrue(controller.contains("isPauseDialogShowing"))
        assertTrue(controller.contains("mCountdownCoordinator.start()"))
        assertTrue(controller.contains("mCountdownCoordinator.stop()"))
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
