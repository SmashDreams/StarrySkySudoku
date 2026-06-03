package com.bird.starryskysudoku.ui.splash

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppEntryActivityStructureTest {
    @Test
    fun splashFallsBackToDeviceLocaleWhenNoSavedLanguageExists() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/splash/AppEntryActivity.kt").readText()
        val app = File("src/main/java/com/bird/starryskysudoku/StarrySkySudokuApp.kt").readText()

        assertTrue(source.contains("readSavedLanguage()"))
        assertTrue(source.contains("Locale.getDefault().language"))
        assertTrue(source.contains("prefs.contains(AppSettings.KEY_LANGUAGE)"))
        assertTrue(app.contains("PlayMusic.getInstance().init(this)"))
    }
}
