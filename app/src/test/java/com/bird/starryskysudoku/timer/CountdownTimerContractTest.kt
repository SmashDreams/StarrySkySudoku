package com.bird.starryskysudoku.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownTimerContractTest {

    @Test
    fun countdownBroadcastUsesBirdActionAndLowercaseExtras() {
        assertEquals("com.bird.action.SUDOKU_COUNTDOWN_TICK", CountdownTimerContract.ACTION_COUNTDOWN_TICK)
        assertEquals("remaining_seconds", CountdownTimerContract.EXTRA_REMAINING_SECONDS)
        assertEquals("initial_seconds", CountdownTimerContract.EXTRA_INITIAL_SECONDS)
    }

    @Test
    fun normalizeInitialSecondsKeepsServiceWithinOneGameWindow() {
        assertEquals(0, CountdownTimerContract.normalizeInitialSeconds(-1))
        assertEquals(300, CountdownTimerContract.normalizeInitialSeconds(300))
        assertEquals(CountdownTimerContract.DEFAULT_TOTAL_SECONDS, CountdownTimerContract.normalizeInitialSeconds(999))
    }

    @Test
    fun notificationHelpersFormatLevelTitleAndRemainingTime() {
        assertEquals("level_number", CountdownTimerContract.EXTRA_LEVEL_NUMBER)
        assertEquals("sudoku_countdown", CountdownTimerContract.NOTIFICATION_CHANNEL_ID)
        assertEquals(1001, CountdownTimerContract.NOTIFICATION_ID)
        assertEquals("第 3 关", CountdownTimerContract.formatLevelTitle(3))
        assertEquals("第 1 关", CountdownTimerContract.formatLevelTitle(-1))
        assertEquals("10:00", CountdownTimerContract.formatRemainingTime(600))
        assertEquals("00:09", CountdownTimerContract.formatRemainingTime(9))
        assertEquals("00:00", CountdownTimerContract.formatRemainingTime(-5))
    }

}
