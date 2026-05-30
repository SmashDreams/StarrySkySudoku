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
