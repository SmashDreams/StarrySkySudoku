package com.bird.starryskysudoku.ui.guide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GuideSpotlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mHighlights = mutableListOf<RectF>()
    private var mDimmed = false
    private val mDimPaint = Paint().apply { color = Color.argb(230, 0, 0, 0) }
    private val mClearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val mStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    fun setHighlights(highlights: List<RectF>, dimmed: Boolean = highlights.isNotEmpty()) {
        mHighlights.clear()
        mHighlights.addAll(highlights)
        mDimmed = dimmed
        visibility = if (mDimmed) VISIBLE else GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!mDimmed) return

        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), mDimPaint)
        for (highlight in mHighlights) {
            canvas.drawRoundRect(highlight, 4f, 4f, mClearPaint)
        }
        canvas.restoreToCount(layer)

        for (highlight in mHighlights) {
            canvas.drawRoundRect(highlight, 4f, 4f, mStrokePaint)
        }
    }
}
