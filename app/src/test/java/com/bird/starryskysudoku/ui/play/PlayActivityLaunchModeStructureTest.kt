package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class PlayActivityLaunchModeStructureTest {
    @Test
    fun playActivityDoesNotUseSingleTaskBecauseRestartNeedsANewInstance() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
            .replace(Regex("\\s+"), " ")

        assertFalse(
            manifest.contains("""android:name=".ui.play.PlayActivity" android:launchMode="singleTask"""")
        )
    }
}
