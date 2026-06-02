package com.bird.starryskysudoku.ui.map

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
    fun createForHomeHasNoFlashExtra() {
        val intent = MapRoute.create(RuntimeEnvironment.getApplication())

        assertFalse(intent.hasExtra("flash_home"))
    }

    @Test
    fun createAfterWinAddsCompletedAndNextLevelWhenPresent() {
        val intent = MapRoute.createAfterWin(
            RuntimeEnvironment.getApplication(),
            completedLevel = 7,
            nextLevel = 8
        )

        assertEquals("7", intent.getStringExtra(MapRoute.EXTRA_COMPLETED_LEVEL))
        assertEquals("8", intent.getStringExtra(MapRoute.EXTRA_NEXT_LEVEL))
        assertFalse(intent.hasExtra("flash_home"))
    }

    @Test
    fun createAfterLoseAddsLoseLevelWithoutFlashExtra() {
        val intent = MapRoute.createAfterLose(RuntimeEnvironment.getApplication(), level = 9)

        assertEquals("9", intent.getStringExtra(MapRoute.EXTRA_LOSE_LEVEL))
        assertFalse(intent.hasExtra("flash_home"))
    }

    @Test
    fun returnAnchorCanBeCopiedBetweenPlayAndMapRoutes() {
        val playIntent = MapRoute.putReturnAnchor(
            MapRoute.create(RuntimeEnvironment.getApplication()),
            adapterPosition = 8,
            topOffsetPx = -42
        )
        val mapIntent = MapRoute.copyReturnAnchor(
            MapRoute.createAfterWin(RuntimeEnvironment.getApplication(), completedLevel = 5, nextLevel = 6),
            playIntent
        )

        assertEquals(8, mapIntent.getIntExtra(MapRoute.EXTRA_RETURN_ANCHOR_POSITION, -1))
        assertEquals(-42, mapIntent.getIntExtra(MapRoute.EXTRA_RETURN_ANCHOR_OFFSET, 0))
    }
}
