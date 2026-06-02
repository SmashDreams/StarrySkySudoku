package com.bird.starryskysudoku.ui.play

class GameResultRecordGate {
    private var mRecorded = false

    fun markIfFirst(levelNum: Int, completed: Boolean): Boolean {
        // 一局结束可能被多个观察者同时触发，这里只放行第一次记录请求。
        if (mRecorded) return false
        mRecorded = true
        return true
    }

    fun unmark() {
        mRecorded = false
    }
}
