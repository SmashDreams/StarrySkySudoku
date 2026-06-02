package com.bird.starryskysudoku.ui.play

class TagData {
    // 用固定长度数组保存一到九的候选数，便于和数字键索引直接对应。
    val mTags: Array<String> = Array(9) { "0" }

    fun setTag(number: String) {
        val idx = number.toIntOrNull() ?: return
        if (idx in 1..9) mTags[idx - 1] = number
    }

    fun deleteTag(number: String) {
        val idx = number.toIntOrNull() ?: return
        if (idx in 1..9) mTags[idx - 1] = "0"
    }

    fun haveTag(number: String): Boolean {
        val idx = number.toIntOrNull() ?: return false
        return idx in 1..9 && mTags[idx - 1] != "0"
    }
}
