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

    fun putReturnAnchor(intent: Intent, adapterPosition: Int, topOffsetPx: Int): Intent {
        return intent
            .putExtra(EXTRA_RETURN_ANCHOR_POSITION, adapterPosition)
            .putExtra(EXTRA_RETURN_ANCHOR_OFFSET, topOffsetPx)
    }

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

    private const val RECYCLER_VIEW_NO_POSITION = -1
}
