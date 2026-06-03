package com.bird.starryskysudoku.ui.guide

import kotlin.math.min
import kotlin.math.roundToInt

object GuideDesignCanvas {
    // 教学页所有聚焦坐标都以这套设计稿尺寸为基准做缩放。
    const val DESIGN_WIDTH = 720
    const val DESIGN_HEIGHT = 1280

    data class Frame(
        val mScale: Float,
        val mWidth: Int,
        val mHeight: Int,
        val mLeft: Int,
        val mTop: Int
    )

    fun fit(parentWidth: Int, parentHeight: Int): Frame {
        if (parentWidth <= 0 || parentHeight <= 0) {
            return Frame(mScale = 0f, mWidth = 0, mHeight = 0, mLeft = 0, mTop = 0)
        }

        // 始终保持完整设计稿可见，多余空间留给四周留白。
        val scale = min(
            parentWidth.toFloat() / DESIGN_WIDTH,
            parentHeight.toFloat() / DESIGN_HEIGHT
        )
        val width = (DESIGN_WIDTH * scale).roundToInt()
        val height = (DESIGN_HEIGHT * scale).roundToInt()
        return Frame(
            mScale = scale,
            mWidth = width,
            mHeight = height,
            mLeft = (parentWidth - width) / 2,
            mTop = (parentHeight - height) / 2
        )
    }
}
