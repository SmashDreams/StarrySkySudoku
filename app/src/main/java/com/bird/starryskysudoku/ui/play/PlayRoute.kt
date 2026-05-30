package com.bird.starryskysudoku.ui.play

import android.content.Context
import android.content.Intent

object PlayRoute {
    const val EXTRA_LEVEL = "num"
    const val EXTRA_LEGACY_LEVEL = "mNum"
    const val EXTRA_USERNAME = "username"
    private const val MIN_LEVEL = 1
    private const val MAX_LEVEL = 40

    fun create(context: Context, level: Int, username: String): Intent {
        return Intent(context, PlayActivity::class.java)
            .putExtra(EXTRA_LEVEL, level.coerceIn(MIN_LEVEL, MAX_LEVEL).toString())
            .putExtra(EXTRA_USERNAME, username)
    }

    fun readLevel(intent: Intent): Int {
        return parseLevel(
            intent.getStringExtra(EXTRA_LEGACY_LEVEL) ?: intent.getStringExtra(EXTRA_LEVEL)
        )
    }

    fun readUsername(intent: Intent): String? {
        return intent.getStringExtra(EXTRA_USERNAME)?.takeIf { it.isNotBlank() }
    }

    fun parseLevel(raw: String?): Int {
        return raw?.toIntOrNull()?.takeIf { it in MIN_LEVEL..MAX_LEVEL } ?: MIN_LEVEL
    }
}
