package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GameplayUsernameWiringSourceTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun mapActivityPassesCurrentUsernameToEveryPlayActivityIntent() {
        val source = mSourceRoot.resolve("ui/map/MapActivity.kt").readText()

        assertTrue(source.contains("PlayRoute.create(this@MapActivity"))
        assertTrue(source.contains("mCurrentUsername"))
    }

    @Test
    fun playActivityAcceptsUsernameExtraAndReadsResultUsername() {
        val source = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val recorderSource = mSourceRoot.resolve("ui/play/GameResultRecorder.kt").readText()

        assertTrue(
            "PlayActivity must expose EXTRA_USERNAME",
            source.contains("const val EXTRA_USERNAME = PlayRoute.EXTRA_USERNAME")
        )
        assertTrue(
            "PlayActivity must prefer username intent extra and fall back to session reader",
            source.contains("mCurrentUsername = PlayRoute.readUsername(intent)") &&
                source.contains("""?: LauncherSessionReader.readUsername(contentResolver)""")
        )
        assertTrue(
            "PlayActivity must read inserted result username from provider cursor",
            recorderSource.contains("GameResultContract.Results.COLUMN_USERNAME")
        )
    }

    @Test
    fun playActivityPassesCurrentUsernameToInternalPlayActivityIntents() {
        val source = mSourceRoot.resolve("ui/play/PlayDialogController.kt").readText()

        assertTrue(
            "Expected PlayDialogController to create internal PlayActivity intents",
            source.contains("PlayRoute.create(mActivity")
        )
        assertTrue(source.contains("mGetUsername()"))
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
