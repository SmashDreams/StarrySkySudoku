package com.bird.starryskysudoku.ui.map

import android.content.Context
import android.content.Intent

object MapRoute {
    // 地图页通过这些参数区分来自首页、胜利页和失败页的不同滚动与提示需求。
    const val EXTRA_FLASH_HOME = "flash_home"
    const val EXTRA_ROLL_LEVEL = "roll"
    const val EXTRA_NEXT_LEVEL = "next"
    const val EXTRA_LOSE_LEVEL = "lose"

    fun create(context: Context, flashHome: Boolean = false): Intent {
        return Intent(context, MapActivity::class.java)
            .putExtra(EXTRA_FLASH_HOME, flashHome)
    }

    fun createForLevel(context: Context, level: Int, flashHome: Boolean): Intent {
        return create(context, flashHome)
            .putExtra(EXTRA_ROLL_LEVEL, level.toString())
    }

    fun createAfterWin(context: Context, nextLevel: Int?, flashHome: Boolean): Intent {
        return create(context, flashHome).apply {
            if (nextLevel != null) putExtra(EXTRA_NEXT_LEVEL, nextLevel.toString())
        }
    }

    fun createAfterLose(context: Context, level: Int, flashHome: Boolean): Intent {
        return create(context, flashHome)
            .putExtra(EXTRA_LOSE_LEVEL, level.toString())
    }

    fun consumeHomeFlashRequest(intent: Intent, fromPrefs: Boolean): Boolean {
        val fromIntent = intent.getBooleanExtra(EXTRA_FLASH_HOME, false)
        // 读取后立即清掉一次性参数，避免页面复用时重复闪烁。
        intent.removeExtra(EXTRA_FLASH_HOME)
        return fromIntent || fromPrefs
    }
}
