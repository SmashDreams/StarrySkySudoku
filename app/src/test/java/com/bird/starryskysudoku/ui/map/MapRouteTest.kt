package com.bird.starryskysudoku.ui.map

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class MapRouteTest {
    @Test
    fun createForHomeFlashAddsFlashExtra() {
        val intent = MapRoute.create(RuntimeEnvironment.getApplication(), flashHome = true)

        assertTrue(intent.getBooleanExtra(MapRoute.EXTRA_FLASH_HOME, false))
    }

    @Test
    fun createAfterWinAddsNextLevelWhenPresent() {
        val intent = MapRoute.createAfterWin(RuntimeEnvironment.getApplication(), nextLevel = 8, flashHome = true)

        assertEquals("8", intent.getStringExtra(MapRoute.EXTRA_NEXT_LEVEL))
        assertTrue(intent.getBooleanExtra(MapRoute.EXTRA_FLASH_HOME, false))
    }

    @Test
    fun createAfterLoseAddsLoseLevelAndFlashExtra() {
        val intent = MapRoute.createAfterLose(RuntimeEnvironment.getApplication(), level = 9, flashHome = true)

        assertEquals("9", intent.getStringExtra(MapRoute.EXTRA_LOSE_LEVEL))
        assertTrue(intent.getBooleanExtra(MapRoute.EXTRA_FLASH_HOME, false))
    }

    @Test
    fun consumeHomeFlashRequestRemovesIntentExtraAndReadsPrefsFlag() {
        val intent = Intent().putExtra(MapRoute.EXTRA_FLASH_HOME, true)

        assertTrue(MapRoute.consumeHomeFlashRequest(intent, fromPrefs = false))
        assertFalse(intent.hasExtra(MapRoute.EXTRA_FLASH_HOME))
        assertTrue(MapRoute.consumeHomeFlashRequest(Intent(), fromPrefs = true))
    }
}
