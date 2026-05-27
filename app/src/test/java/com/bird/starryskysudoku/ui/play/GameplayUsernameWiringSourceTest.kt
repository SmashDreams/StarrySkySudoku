package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GameplayUsernameWiringSourceTest {
    private val sourceRoot = locateSourceRoot()

    @Test
    fun mapActivityPassesCurrentUsernameToEveryPlayActivityIntent() {
        val source = sourceRoot.resolve("ui/map/MapActivity.kt").readText()
        val playIntentPattern = Regex(
            """Intent\(this(?:@MapActivity)?,\s*PlayActivity::class\.java\)([\s\S]*?)(?:R\.anim|finish\(\)|MyDialogManager|$)"""
        )
        val playIntents = playIntentPattern.findAll(source).toList()

        assertTrue("Expected MapActivity to create PlayActivity intents", playIntents.isNotEmpty())
        playIntents.forEach { match ->
            assertTrue(
                "PlayActivity intent must include current username: ${match.value}",
                match.value.contains(".putExtra(PlayActivity.EXTRA_USERNAME, mCurrentUsername)")
            )
        }
    }

    @Test
    fun playActivityAcceptsUsernameExtraAndReadsResultUsername() {
        val source = sourceRoot.resolve("ui/play/PlayActivity.kt").readText()

        assertTrue(
            "PlayActivity must expose EXTRA_USERNAME",
            source.contains("""const val EXTRA_USERNAME = "username"""")
        )
        assertTrue(
            "PlayActivity must prefer username intent extra and fall back to session reader",
            source.contains(
                """mCurrentUsername = intent.getStringExtra(EXTRA_USERNAME)"""
            ) && source.contains("""?: LauncherSessionReader.readUsername(contentResolver)""")
        )
        assertTrue(
            "PlayActivity must read inserted result username from provider cursor",
            source.contains("GameResultContract.Results.COLUMN_USERNAME")
        )
    }

    @Test
    fun playActivityPassesCurrentUsernameToInternalPlayActivityIntents() {
        val source = sourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val internalPlayIntentPattern = Regex(
            """Intent\(this,\s*PlayActivity::class\.java\)([\s\S]*?)(?:R\.anim|$)"""
        )
        val internalPlayIntents = internalPlayIntentPattern.findAll(source).toList()

        assertTrue(
            "Expected PlayActivity to create internal PlayActivity intents",
            internalPlayIntents.isNotEmpty()
        )
        internalPlayIntents.forEach { match ->
            assertTrue(
                "Internal PlayActivity intent must preserve current username: ${match.value}",
                match.value.contains(".putExtra(EXTRA_USERNAME, mCurrentUsername)")
            )
        }
    }

    private fun locateSourceRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        while (dir.parentFile != null) {
            val sourceRoot = dir.resolve("src/main/java/com/bird/starryskysudoku")
            if (sourceRoot.isDirectory) return sourceRoot
            val appSourceRoot = dir.resolve("app/src/main/java/com/bird/starryskysudoku")
            if (appSourceRoot.isDirectory) return appSourceRoot
            dir = dir.parentFile
        }
        error("Unable to locate app source root")
    }
}
