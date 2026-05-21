package com.bird.starryskysudoku.timer

object CountdownTimerContract {
    const val ACTION_COUNTDOWN_TICK = "com.bird.action.SUDOKU_COUNTDOWN_TICK"
    const val EXTRA_REMAINING_SECONDS = "remaining_seconds"
    const val EXTRA_INITIAL_SECONDS = "initial_seconds"
    const val DEFAULT_TOTAL_SECONDS = 600

    fun normalizeInitialSeconds(seconds: Int): Int {
        /*
         * 倒计时初始值来自启动参数，必须裁剪到单局游戏时长内。
         * 这样可以避免异常参数导致后台服务长时间运行。
         */
        return seconds.coerceIn(0, DEFAULT_TOTAL_SECONDS)
    }
}
