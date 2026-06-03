package com.bird.starryskysudoku.ui.play

class GameResultRecordGate {
    // 记录闸门只关心“这一局是否已经写过战绩”，不区分胜利还是失败来源。
    private var mRecorded = false

    fun markIfFirst(levelNum: Int, completed: Boolean): Boolean {
        // 一局结束可能被多个观察者同时触发，这里只放行第一次记录请求。
        if (mRecorded) return false
        mRecorded = true
        return true
    }

    fun unmark() {
        // 写入失败时放开闸门，允许后续重试重新落库。
        mRecorded = false
    }
}
