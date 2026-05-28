package com.bird.starryskysudoku

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsContractTest {
    @Test
    fun languagePreferencesUseOneSharedNameAndKey() {
        assertEquals("language", AppSettings.PREFS_LANGUAGE)
        assertEquals("language", AppSettings.KEY_LANGUAGE)
        assertEquals("zh", AppSettings.DEFAULT_LANGUAGE)
    }

    @Test
    fun musicPreferencesExposeSharedKeys() {
        assertEquals("music_set", AppSettings.PREFS_MUSIC)
        assertEquals("music", AppSettings.KEY_MUSIC)
        assertEquals("audio", AppSettings.KEY_AUDIO)
    }
}
