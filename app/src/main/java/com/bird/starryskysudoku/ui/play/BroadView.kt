package com.bird.starryskysudoku.ui.play

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatImageView
import com.bird.starryskysudoku.R

class BroadView : AppCompatImageView {

    companion object {
        const val SELECT_NONE = 0
        const val SELECT_ON = 1
        const val BE_SELECTED = -1
        const val WRONG = 2
        const val PROBLEM = 1
        const val EMPTY = 0
    }

    private var mData: Array<Array<PlayViewModel.CellData>> = Array(9) { Array(9) { PlayViewModel.CellData(0, 0, "0", 0) } }
    private var mTagData: Array<Array<TagData?>> = Array(9) { arrayOfNulls(9) }
    private var mWidth = 0f
    private var mHeight = 0f
    private var mRow = 0
    private var mCol = 0
    private var mBigBlock = 0
    private var mTextSize = 80
    private var mTextAlpha = 255
    private var mLine = 0
    private var mWrong = false
    private var mHasDone = false
    private var mListener: Listener? = null

    private lateinit var mProblemNormal: Bitmap
    private lateinit var mProblemLight: Bitmap
    private lateinit var mProblemWrong: Bitmap
    private lateinit var mEmptyNormal: Bitmap
    private lateinit var mEmptyLight: Bitmap
    private lateinit var mEmptyWrong: Bitmap
    private lateinit var mEmptySelected: Bitmap

    interface Listener {
        fun onTouch(row: Int, col: Int, block: Int)
    }

    constructor(@NonNull context: Context) : super(context) { initView() }
    constructor(@NonNull context: Context, @NonNull attrs: AttributeSet) : super(context, attrs) { initView() }
    constructor(@NonNull context: Context, @NonNull attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initView() }

    fun setListener(listener: Listener) { mListener = listener }
    fun setTextSize(size: Int) { mTextSize = size; invalidate() }
    fun setTextAlpha(alpha: Int) { mTextAlpha = alpha; invalidate() }
    fun setLine(line: Int) { mLine = line }

    private fun initView() {
        mProblemNormal = BitmapFactory.decodeResource(resources, R.drawable.ic_play_grid_filled_off)
        mProblemLight = BitmapFactory.decodeResource(resources, R.drawable.ic_play_grid_filled_on)
        mProblemWrong = BitmapFactory.decodeResource(resources, R.drawable.ic_play_grid_filled_clash)
        mEmptyNormal = BitmapFactory.decodeResource(resources, R.drawable.ic_play_grid_none_off)
        mEmptyLight = BitmapFactory.decodeResource(resources, R.drawable.ic_play_grid_none_on)
        mEmptyWrong = BitmapFactory.decodeResource(resources, R.drawable.ic_play_grid_focus_clash)
        mEmptySelected = BitmapFactory.decodeResource(resources, R.drawable.ic_play_grid_focus_off)
    }

    fun initData(data: Array<Array<PlayViewModel.CellData>>) { mData = data }
    fun initTagData(tagData: Array<Array<TagData?>>) { mTagData = tagData }
    fun overDone(data: Array<Array<PlayViewModel.CellData>>) { mData = data; mHasDone = true }
    fun setWrong(wrong: Boolean) { mWrong = wrong }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mWidth = (w - 54) / 9.0f
        mHeight = (h - 54) / 9.0f
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = Rect()
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                rect.set(
                    (28 + (mWidth * j)).toInt(), (28 + (mWidth * i)).toInt(),
                    (28 + (mWidth * (j + 1))).toInt(), (28 + (mWidth * (i + 1))).toInt()
                )
                if (mData[i][j].type == PROBLEM) {
                    when {
                        mData[i][j].status == SELECT_ON || mData[i][j].status == BE_SELECTED ->
                            canvas.drawBitmap(mProblemLight, null, rect, Paint())
                        mData[i][j].status == WRONG ->
                            canvas.drawBitmap(mProblemWrong, null, rect, Paint())
                        else -> canvas.drawBitmap(mProblemNormal, null, rect, Paint())
                    }
                } else if (mData[i][j].type == EMPTY) {
                    when {
                        mData[i][j].status == BE_SELECTED ->
                            canvas.drawBitmap(mEmptySelected, null, rect, Paint())
                        mData[i][j].status == SELECT_ON ->
                            canvas.drawBitmap(mEmptyLight, null, rect, Paint())
                        mData[i][j].status == WRONG ->
                            canvas.drawBitmap(mEmptyWrong, null, rect, Paint())
                        else -> canvas.drawBitmap(mEmptyNormal, null, rect, Paint())
                    }
                }
            }
        }
    }

    override fun dispatchDraw(@NonNull canvas: Canvas) {
        super.dispatchDraw(canvas)
        val tag = Paint().apply {
            textSize = 33f; color = Color.GRAY
            typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val subWidth = mWidth / 3
        val subHeight = mHeight / 3

        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (mData[i][j].value == "0") {
                    val cellLeft = 28 + mWidth * j
                    val cellTop = 28 + mWidth * i
                    var position = 0
                    for (m in 0 until 3) {
                        for (n in 0 until 3) {
                            if (mTagData[i][j]?.tags?.get(position) != "0") {
                                val subCenterX = cellLeft + n * subWidth + subWidth / 2
                                val subCenterY = cellTop + m * subHeight + subHeight / 2
                                val baseline = subCenterY - (tag.ascent() + tag.descent()) / 2
                                canvas.drawText(mTagData[i][j]!!.tags[position], subCenterX, baseline, tag)
                            }
                            position++
                        }
                    }
                }
            }
        }
    }

    override fun onDrawForeground(@NonNull canvas: Canvas) {
        super.onDrawForeground(canvas)
        val number = Paint().apply {
            isAntiAlias = true; color = Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        if (!mHasDone) {
            number.textSize = 80f; number.typeface = Typeface.DEFAULT_BOLD
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (mData[i][j].value != "0") {
                        val cellLeft = 28 + mWidth * j; val cellTop = 28 + mWidth * i
                        val centerX = cellLeft + mWidth / 2; val centerY = cellTop + mWidth / 2
                        val baseline = centerY - (number.ascent() + number.descent()) / 2
                        canvas.drawText(mData[i][j].value, centerX, baseline, number)
                    }
                }
            }
        } else {
            number.textSize = 80f; number.typeface = Typeface.DEFAULT_BOLD
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    val cellLeft = 28 + mWidth * j; val cellTop = 28 + mWidth * i
                    val centerX = cellLeft + mWidth / 2; val centerY = cellTop + mWidth / 2
                    if (i == mLine) {
                        number.textSize = mTextSize.toFloat(); number.alpha = mTextAlpha
                    } else {
                        number.textSize = 80f; number.alpha = 255
                    }
                    val baseline = centerY - (number.ascent() + number.descent()) / 2
                    canvas.drawText(mData[i][j].value, centerX, baseline, number)
                }
            }
        }

        // Grid lines
        val rectangular = Paint().apply {
            color = Color.BLACK; strokeWidth = 1f; style = Paint.Style.STROKE
        }
        val rect = Rect()
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                rect.set(
                    (28 + (mWidth * j)).toInt(), (28 + (mWidth * i)).toInt(),
                    (28 + (mWidth * (j + 1))).toInt(), (28 + (mWidth * (i + 1))).toInt()
                )
                canvas.drawRect(rect, rectangular)
            }
        }

        // Big block lines (3x3)
        val line = Paint().apply {
            color = Color.BLACK; strokeWidth = 9f
            style = Paint.Style.STROKE; isAntiAlias = true
        }
        val lineOffset = line.strokeWidth / 2
        for (i in 0 until 4) {
            val y = 28 - lineOffset + (mWidth * i * 3)
            canvas.drawLine(28 - lineOffset, y, 28 + (mWidth * 9) + lineOffset, y, line)
        }
        for (i in 0 until 4) {
            val x = 28 - lineOffset + (mWidth * i * 3)
            canvas.drawLine(x, 28 - lineOffset, x, 28 + (mWidth * 9) + lineOffset, line)
        }

        // Outer border
        val outerBorder = Paint().apply {
            color = Color.WHITE; strokeWidth = 2f
            style = Paint.Style.STROKE; isAntiAlias = true
        }
        val borderMargin = 24
        val outerRect = Rect(
            borderMargin, borderMargin,
            (borderMargin + mWidth * 9).toInt(), (borderMargin + mWidth * 9).toInt()
        )
        canvas.drawRect(outerRect, outerBorder)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mHasDone || mWrong) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x; val y = event.y
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (x > (28 + (mWidth * j)) && x < (28 + (mWidth * (j + 1)))) {
                        if (y > (28 + (mWidth * i)) && y < (28 + (mWidth * (i + 1)))) {
                            mRow = i; mCol = j
                            mBigBlock = getBigBlock(i, j)
                            break
                        }
                    }
                }
            }
            mListener?.onTouch(mRow, mCol, mBigBlock)
        }
        return super.onTouchEvent(event)
    }

    fun getBigBlock(row: Int, col: Int): Int {
        if (row < 0 || row > 8 || col < 0 || col > 8) return 0
        return (row / 3) * 3 + (col / 3) + 1
    }
}
