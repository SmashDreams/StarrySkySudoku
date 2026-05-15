package com.bird.starryskysudoku.ui.play

class TagData {
    val tags: Array<String> = Array(9) { "0" }

    fun setTag(number: String) {
        val idx = number.toIntOrNull() ?: return
        if (idx in 1..9) tags[idx - 1] = number
    }

    fun deleteTag(number: String) {
        val idx = number.toIntOrNull() ?: return
        if (idx in 1..9) tags[idx - 1] = "0"
    }

    fun haveTag(number: String): Boolean {
        val idx = number.toIntOrNull() ?: return false
        return idx in 1..9 && tags[idx - 1] != "0"
    }
}
