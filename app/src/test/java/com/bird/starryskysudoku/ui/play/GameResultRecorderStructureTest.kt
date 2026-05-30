package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GameResultRecorderStructureTest {
    @Test
    fun playActivityDelegatesProviderWriteToRecorder() {
        val playActivity = File("src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt").readText()
        val recorder = File("src/main/java/com/bird/starryskysudoku/ui/play/GameResultRecorder.kt").readText()

        assertTrue(playActivity.contains("GameResultRecorder(contentResolver)"))
        assertTrue(playActivity.contains("mGameResultRecorder.saveAndVerify"))
        assertFalse(playActivity.contains("contentResolver.insert(GameResultContract.Results.CONTENT_URI"))
        assertTrue(recorder.contains("GameResultContract.Results.toContentValues"))
    }
}
