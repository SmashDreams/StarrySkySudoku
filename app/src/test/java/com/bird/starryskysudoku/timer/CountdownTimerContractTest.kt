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
}
