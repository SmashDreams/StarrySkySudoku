package com.bird.starryskysudoku.ui.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.repository.PassStatus
import com.bird.starryskysudoku.ui.play.PlayRoute
import kotlin.math.hypot

class MapPathOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 路径层只负责虚线连接关系，星星图片和数字文本仍由列表项自己绘制。
    private val mPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(PATH_WIDTH_DP)
        strokeCap = Paint.Cap.ROUND
        pathEffect = DashPathEffect(floatArrayOf(dpToPx(DASH_DP), dpToPx(GAP_DP)), 0f)
    }
    private var mStars: Array<ImageView> = emptyArray()
    private var mRow: Array<MapEntity?> = emptyArray()
    private var mPreviousRowBelow: Array<MapEntity?>? = null
    private var mHasNextRowAbove = false

    fun bind(
        stars: Array<ImageView>,
        row: Array<MapEntity?>,
        previousRowBelow: Array<MapEntity?>?,
        hasNextRowAbove: Boolean
    ) {
        // 绑定当前行与相邻行数据后统一重绘，避免适配器为每条连接线单独维护视图。
        mStars = stars
        mRow = row
        mPreviousRowBelow = previousRowBelow
        mHasNextRowAbove = hasNextRowAbove
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centers = mStars.map { star ->
            PointF(star.x + star.width / 2f, star.y + star.height / 2f)
        }

        for (index in 0 until 3) {
            drawSegment(
                canvas = canvas,
                from = centers[index],
                to = centers[index + 1],
                unlocked = mRow[index]?.mStatus == PassStatus.COMPLETED,
                trimStart = true,
                trimEnd = true
            )
        }

        drawIncomingConnector(canvas, centers)
        drawOutgoingConnector(canvas, centers)
    }

    private fun drawIncomingConnector(canvas: Canvas, centers: List<PointF>) {
        val previousRow = mPreviousRowBelow ?: return
        // 每一行都从下方上一行的第四颗星延续过来，所以入口固定连向当前行第一颗星。
        val boundary = PointF(crossRowBoundaryX(centers), height.toFloat())
        drawSegment(
            canvas = canvas,
            from = boundary,
            to = centers.first(),
            unlocked = previousRow.getOrNull(3)?.mStatus == PassStatus.COMPLETED,
            trimStart = false,
            trimEnd = true
        )
    }

    private fun drawOutgoingConnector(canvas: Canvas, centers: List<PointF>) {
        val lastLevel = mRow.getOrNull(3)?.mPassNum ?: return
        if (!mHasNextRowAbove || lastLevel >= PlayRoute.MAX_LEVEL) return

        // 当前行最后一颗星如果不是最终关，就向上接到下一行入口形成连续路径。
        val boundary = PointF(crossRowBoundaryX(centers), 0f)
        drawSegment(
            canvas = canvas,
            from = centers.last(),
            to = boundary,
            unlocked = mRow.getOrNull(3)?.mStatus == PassStatus.COMPLETED,
            trimStart = true,
            trimEnd = false
        )
    }

    private fun crossRowBoundaryX(centers: List<PointF>): Float {
        val first = centers.first()
        val last = centers.last()
        val nextFirstY = first.y - height
        val denominator = nextFirstY - last.y
        if (denominator == 0f) return (first.x + last.x) / 2f
        // 用当前行末星和下一行首星的连线，反推它与上下边界的交点位置。
        val progressToTopBoundary = (0f - last.y) / denominator
        return last.x + (first.x - last.x) * progressToTopBoundary
    }

    private fun drawSegment(
        canvas: Canvas,
        from: PointF,
        to: PointF,
        unlocked: Boolean,
        trimStart: Boolean,
        trimEnd: Boolean
    ) {
        val trimmed = trimSegment(from, to, trimStart, trimEnd) ?: return
        mPathPaint.color = if (unlocked) UNLOCKED_COLOR else LOCKED_COLOR
        canvas.drawLine(
            trimmed.first.x,
            trimmed.first.y,
            trimmed.second.x,
            trimmed.second.y,
            mPathPaint
        )
    }

    private fun trimSegment(
        from: PointF,
        to: PointF,
        trimStart: Boolean,
        trimEnd: Boolean
    ): Pair<PointF, PointF>? {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = hypot(dx, dy)
        val inset = dpToPx(STAR_EDGE_INSET_DP)

        val ux = dx / length
        val uy = dy / length
        // 线段两端向内收一点，避免虚线直接扎进星星贴图内部。
        return PointF(
            from.x + ux * (if (trimStart) inset else 0f),
            from.y + uy * (if (trimStart) inset else 0f)
        ) to PointF(
            to.x - ux * (if (trimEnd) inset else 0f),
            to.y - uy * (if (trimEnd) inset else 0f)
        )
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    private companion object {
        private const val PATH_WIDTH_DP = 4f
        private const val DASH_DP = 10f
        private const val GAP_DP = 10f
        private const val STAR_EDGE_INSET_DP = 38f
        private val UNLOCKED_COLOR = Color.rgb(249, 217, 47)
        private val LOCKED_COLOR = Color.argb(180, 220, 224, 232)
    }
}
