package com.bird.starryskysudoku.ui.play

class GameResultRecordGate {
    private var mRecorded = false

    fun markIfFirst(levelNum: Int, completed: Boolean): Boolean {
        if (mRecorded) return false
        mRecorded = true
        return true
    }

    fun unmark() {
        mRecorded = false
    }
}
