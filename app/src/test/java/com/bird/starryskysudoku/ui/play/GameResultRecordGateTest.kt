package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameResultRecordGateTest {
    @Test
    fun markRecordedAllowsOnlyOneResultForCurrentGame() {
        val gate = GameResultRecordGate()

        assertTrue(gate.markIfFirst(levelNum = 1, completed = true))
        assertFalse(gate.markIfFirst(levelNum = 1, completed = true))
        assertFalse(gate.markIfFirst(levelNum = 1, completed = false))
    }

    @Test
    fun unmarkAllowsRetryWhenPersistenceFails() {
        val gate = GameResultRecordGate()

        assertTrue(gate.markIfFirst(levelNum = 1, completed = false))
        gate.unmark()

        assertTrue(gate.markIfFirst(levelNum = 1, completed = false))
    }
}
