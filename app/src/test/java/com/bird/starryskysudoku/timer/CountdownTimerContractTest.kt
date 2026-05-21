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
}
