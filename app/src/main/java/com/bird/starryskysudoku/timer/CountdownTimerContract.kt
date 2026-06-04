package com.bird.starryskysudoku.timer

import java.util.Locale

object CountdownTimerContract {
    const val ACTION_COUNTDOWN_TICK = "com.bird.action.SUDOKU_COUNTDOWN_TICK"
    const val ACTION_PAUSE_TIMER = "com.bird.action.SUDOKU_PAUSE_TIMER"
    const val ACTION_RESUME_TIMER = "com.bird.action.SUDOKU_RESUME_TIMER"
    const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
    const val EXTRA_INITIAL_SECONDS = "initial_seconds"
    const val EXTRA_LEVEL_NUMBER = "level_number"
    const val EXTRA_USERNAME = "username"
    const val NOTIFICATION_CHANNEL_ID = "sudoku_countdown"
    const val NOTIFICATION_ID = 1001
    const val DEFAULT_TOTAL_SECONDS = 600
    const val MIN_LEVEL_NUMBER = 1

    fun normalizeInitialSeconds(seconds: Int): Int {
        return seconds.coerceIn(0, DEFAULT_TOTAL_SECONDS)
    }

    fun normalizeLevelNumber(levelNumber: Int): Int = levelNumber.coerceAtLeast(MIN_LEVEL_NUMBER)

    fun formatLevelTitle(levelNumber: Int): String {
        return "第 ${normalizeLevelNumber(levelNumber)} 关"
    }

    fun formatRemainingTime(seconds: Int): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        return String.format(Locale.ROOT, "%02d:%02d", safeSeconds / 60, safeSeconds % 60)
    }
}
