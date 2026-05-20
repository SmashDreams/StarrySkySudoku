package com.bird.starryskysudoku.ui.guide

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class GuideDesignCanvasLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var mFrame = GuideDesignCanvas.fit(0, 0)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        mFrame = GuideDesignCanvas.fit(parentWidth, parentHeight)

        val childWidthSpec = MeasureSpec.makeMeasureSpec(mFrame.width, MeasureSpec.EXACTLY)
        val childHeightSpec = MeasureSpec.makeMeasureSpec(mFrame.height, MeasureSpec.EXACTLY)
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility != View.GONE) {
                child.measure(childWidthSpec, childHeightSpec)
            }
        }

        setMeasuredDimension(parentWidth, parentHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility != View.GONE) {
                child.layout(
                    mFrame.left,
                    mFrame.top,
                    mFrame.left + mFrame.width,
                    mFrame.top + mFrame.height
                )
            }
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}
