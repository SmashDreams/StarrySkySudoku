package com.bird.starryskysudoku.ui.map

object MapScrollPolicy {
    fun offsetDpAfterCompletedLevel(completedLevel: Int): Int {
        return when (completedLevel) {
            in 3..5 -> 102
            in 6..40 -> 84
            else -> 0
        }
    }
}
