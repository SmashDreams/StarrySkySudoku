package com.bird.starryskysudoku.ui.play

import android.content.Context
import android.content.Intent

object PlayRoute {
    // 棋盘页兼容旧参数名和新参数名，避免通知入口与页面跳转同时改动时互相影响。
    const val EXTRA_LEVEL = "num"
    const val EXTRA_LEGACY_LEVEL = "mNum"
    const val EXTRA_USERNAME = "username"
    private const val MIN_LEVEL = 1
    private const val MAX_LEVEL = 40

    fun create(context: Context, level: Int, username: String): Intent {
        // 跳转棋盘页时统一在这里裁剪关卡范围并附带当前用户名，避免各入口自行拼参数。
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
        // 空用户名一律视为未携带，交给上层决定是否回退成游客或会话用户。
        return intent.getStringExtra(EXTRA_USERNAME)?.takeIf { it.isNotBlank() }
    }

    fun parseLevel(raw: String?): Int {
        // 所有关卡参数最终都约束在有效范围内，避免异常跳转访问越界关卡。
        return raw?.toIntOrNull()?.takeIf { it in MIN_LEVEL..MAX_LEVEL } ?: MIN_LEVEL
    }
}
