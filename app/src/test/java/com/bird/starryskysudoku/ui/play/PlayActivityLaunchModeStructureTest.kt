package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayActivityLaunchModeStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playActivityDoesNotUseSingleTaskBecauseRestartNeedsANewInstance() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
            .replace(Regex("\\s+"), " ")

        assertFalse(
            manifest.contains("""android:name=".ui.play.PlayActivity" android:launchMode="singleTask"""")
        )
    }

    @Test
    fun debugCompleteEntryIsOnlyRegisteredForDebugBuilds() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()

        assertTrue(activity.contains("if (BuildConfig.DEBUG)"))
        assertTrue(activity.contains("initDebugCompleteButton()"))
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
