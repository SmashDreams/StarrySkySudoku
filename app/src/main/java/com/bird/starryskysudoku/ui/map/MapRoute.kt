package com.bird.starryskysudoku.ui.map

import android.content.Context
import android.content.Intent

object MapRoute {
    // 地图页通过这些参数区分来自首页、胜利页和失败页的不同滚动与提示需求。
    const val EXTRA_ROLL_LEVEL = "roll"
    const val EXTRA_COMPLETED_LEVEL = "completed"
    const val EXTRA_NEXT_LEVEL = "next"
    const val EXTRA_LOSE_LEVEL = "lose"
    const val EXTRA_RETURN_ANCHOR_POSITION = "map_return_anchor_position"
    const val EXTRA_RETURN_ANCHOR_OFFSET = "map_return_anchor_offset"

    fun create(context: Context): Intent {
        return Intent(context, MapActivity::class.java)
    }

    fun createForLevel(context: Context, level: Int): Intent {
        return create(context)
            .putExtra(EXTRA_ROLL_LEVEL, level.toString())
    }

    fun createAfterWin(
        context: Context,
        completedLevel: Int,
        nextLevel: Int?
    ): Intent {
        return create(context).apply {
            putExtra(EXTRA_COMPLETED_LEVEL, completedLevel.toString())
            if (nextLevel != null) putExtra(EXTRA_NEXT_LEVEL, nextLevel.toString())
        }
    }

    fun createAfterLose(context: Context, level: Int): Intent {
        return create(context)
            .putExtra(EXTRA_LOSE_LEVEL, level.toString())
    }

    // 进入棋盘页前记录当前地图锚点，返回时可恢复到离开前看到的位置。
    fun putReturnAnchor(intent: Intent, adapterPosition: Int, topOffsetPx: Int): Intent {
        return intent
            .putExtra(EXTRA_RETURN_ANCHOR_POSITION, adapterPosition)
            .putExtra(EXTRA_RETURN_ANCHOR_OFFSET, topOffsetPx)
    }

    // 部分跳转会重建 Intent，这里把地图滚动锚点一并透传，避免返回后跳到默认位置。
    fun copyReturnAnchor(target: Intent, source: Intent): Intent {
        if (source.hasExtra(EXTRA_RETURN_ANCHOR_POSITION) && source.hasExtra(EXTRA_RETURN_ANCHOR_OFFSET)) {
            val adapterPosition = source.getIntExtra(EXTRA_RETURN_ANCHOR_POSITION, RECYCLER_VIEW_NO_POSITION)
            if (adapterPosition < 0) return target
            putReturnAnchor(
                intent = target,
                adapterPosition = adapterPosition,
                topOffsetPx = source.getIntExtra(EXTRA_RETURN_ANCHOR_OFFSET, 0)
            )
        }
        return target
    }

    // 语言切换等场景会主动清掉旧锚点，避免列表布局变化后复用过期坐标。
    fun clearReturnAnchor(intent: Intent) {
        intent.removeExtra(EXTRA_RETURN_ANCHOR_POSITION)
        intent.removeExtra(EXTRA_RETURN_ANCHOR_OFFSET)
    }

    private const val RECYCLER_VIEW_NO_POSITION = -1
}

// 根据已通关关卡数计算地图列表所需的滚动偏移量。
object MapScrollPolicy {
    fun offsetDpAfterCompletedLevel(completedLevel: Int): Int {
        return when (completedLevel) {
            in 3..5 -> 102
            in 6..40 -> 84
            else -> 0
        }
    }
}
