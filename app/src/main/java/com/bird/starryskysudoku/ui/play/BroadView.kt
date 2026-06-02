package com.bird.starryskysudoku.ui.play

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.Keep
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

    private var mData: Array<Array<BoardCell>> = Array(9) { Array(9) { BoardCell(0, 0, "0", 0) } }
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
    // 背景格子、数字和边框分别使用独立画笔，避免样式互相污染。
    private val mBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val mCellRect = Rect()
    private val mOuterRect = Rect()
    private val mTagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 33f
        color = Color.GRAY
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val mNumberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val mThinBorderPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val mBlockLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 9f
        style = Paint.Style.STROKE
    }
    private val mOuterBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    interface Listener {
        fun onTouch(row: Int, col: Int, block: Int)
    }

    constructor(context: Context) : super(context) { initView() }
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { initView() }
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) { initView() }

    fun setListener(listener: Listener) { mListener = listener }
    @Keep
    fun setTextSize(size: Int) { mTextSize = size; invalidate() }
    @Keep
    fun setTextAlpha(alpha: Int) { mTextAlpha = alpha; invalidate() }
    @Keep
    fun setLine(line: Int) { mLine = line }

    private fun initView() {
        // 不同状态的格子背景预先解码，绘制时只做位图切换，降低重绘负担。
        mProblemNormal = BitmapFactory.decodeResource(resources, R.drawable.sudoku_cell_given)
        mProblemLight = BitmapFactory.decodeResource(resources, R.drawable.sudoku_cell_given_selected)
        mProblemWrong = BitmapFactory.decodeResource(resources, R.drawable.sudoku_cell_given_error)
        mEmptyNormal = BitmapFactory.decodeResource(resources, R.drawable.sudoku_cell_empty)
        mEmptyLight = BitmapFactory.decodeResource(resources, R.drawable.sudoku_cell_empty_related)
        mEmptyWrong = BitmapFactory.decodeResource(resources, R.drawable.sudoku_cell_editable_error)
        mEmptySelected = BitmapFactory.decodeResource(resources, R.drawable.sudoku_cell_editable_focused)
    }

    fun initData(data: Array<Array<BoardCell>>) { mData = data }
    fun initTagData(tagData: Array<Array<TagData?>>) { mTagData = tagData }
    fun overDone(data: Array<Array<BoardCell>>) { mData = data; mHasDone = true }
    fun setWrong(wrong: Boolean) { mWrong = wrong }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 棋盘是正方形素材，宽高都按同一单格尺寸计算。
        mWidth = SudokuBoardGeometry.cellSize(w.toFloat())
        mHeight = SudokuBoardGeometry.cellSize(h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                mCellRect.set(
                    (SudokuBoardGeometry.CELL_INSET + (mWidth * j)).toInt(), (SudokuBoardGeometry.CELL_INSET + (mWidth * i)).toInt(),
                    (SudokuBoardGeometry.CELL_INSET + (mWidth * (j + 1))).toInt(), (SudokuBoardGeometry.CELL_INSET + (mWidth * (i + 1))).toInt()
                )
                if (mData[i][j].mType == PROBLEM) {
                    // 题面数字和玩家填写格使用不同底图，选中与错误态再分开处理。
                    when {
                        mData[i][j].mStatus == SELECT_ON || mData[i][j].mStatus == BE_SELECTED ->
                            canvas.drawBitmap(mProblemLight, null, mCellRect, mBitmapPaint)
                        mData[i][j].mStatus == WRONG ->
                            canvas.drawBitmap(mProblemWrong, null, mCellRect, mBitmapPaint)
                        else -> canvas.drawBitmap(mProblemNormal, null, mCellRect, mBitmapPaint)
                    }
                } else if (mData[i][j].mType == EMPTY) {
                    when {
                        mData[i][j].mStatus == BE_SELECTED ->
                            canvas.drawBitmap(mEmptySelected, null, mCellRect, mBitmapPaint)
                        mData[i][j].mStatus == SELECT_ON ->
                            canvas.drawBitmap(mEmptyLight, null, mCellRect, mBitmapPaint)
                        mData[i][j].mStatus == WRONG ->
                            canvas.drawBitmap(mEmptyWrong, null, mCellRect, mBitmapPaint)
                        else -> canvas.drawBitmap(mEmptyNormal, null, mCellRect, mBitmapPaint)
                    }
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val subWidth = mWidth / 3
        val subHeight = mHeight / 3

        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (mData[i][j].mValue == "0") {
                    // 只有空格才绘制九宫笔记，已填数字不会叠加候选数。
                    val cellLeft = SudokuBoardGeometry.CELL_INSET + mWidth * j
                    val cellTop = SudokuBoardGeometry.CELL_INSET + mWidth * i
                    var position = 0
                    for (m in 0 until 3) {
                        for (n in 0 until 3) {
                            val tagText = mTagData[i][j]?.mTags?.getOrNull(position) ?: "0"
                            if (tagText != "0") {
                                val subCenterX = cellLeft + n * subWidth + subWidth / 2
                                val subCenterY = cellTop + m * subHeight + subHeight / 2
                                val baseline = subCenterY - (mTagPaint.ascent() + mTagPaint.descent()) / 2
                                canvas.drawText(tagText, subCenterX, baseline, mTagPaint)
                            }
                            position++
                        }
                    }
                }
            }
        }
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)

        if (!mHasDone) {
            // 常规状态下所有数字使用统一字号。
            mNumberPaint.textSize = 80f
            mNumberPaint.alpha = 255
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (mData[i][j].mValue != "0") {
                        val cellLeft = SudokuBoardGeometry.CELL_INSET + mWidth * j; val cellTop = SudokuBoardGeometry.CELL_INSET + mWidth * i
                        val centerX = cellLeft + mWidth / 2; val centerY = cellTop + mWidth / 2
                        val baseline = centerY - (mNumberPaint.ascent() + mNumberPaint.descent()) / 2
                        canvas.drawText(mData[i][j].mValue, centerX, baseline, mNumberPaint)
                    }
                }
            }
        } else {
            // 通关动画阶段只对当前行使用外部动画传入的字号和透明度。
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    val cellLeft = SudokuBoardGeometry.CELL_INSET + mWidth * j; val cellTop = SudokuBoardGeometry.CELL_INSET + mWidth * i
                    val centerX = cellLeft + mWidth / 2; val centerY = cellTop + mWidth / 2
                    if (i == mLine) {
                        mNumberPaint.textSize = mTextSize.toFloat(); mNumberPaint.alpha = mTextAlpha
                    } else {
                        mNumberPaint.textSize = 80f; mNumberPaint.alpha = 255
                    }
                    val baseline = centerY - (mNumberPaint.ascent() + mNumberPaint.descent()) / 2
                    canvas.drawText(mData[i][j].mValue, centerX, baseline, mNumberPaint)
                }
            }
        }

        /*
         * 绘制每个小格子的细边框。
         */
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                mCellRect.set(
                    (SudokuBoardGeometry.CELL_INSET + (mWidth * j)).toInt(), (SudokuBoardGeometry.CELL_INSET + (mWidth * i)).toInt(),
                    (SudokuBoardGeometry.CELL_INSET + (mWidth * (j + 1))).toInt(), (SudokuBoardGeometry.CELL_INSET + (mWidth * (i + 1))).toInt()
                )
                canvas.drawRect(mCellRect, mThinBorderPaint)
            }
        }

        /*
         * 绘制三乘三宫格的粗分隔线，帮助玩家区分九宫区域。
         */
        val lineOffset = mBlockLinePaint.strokeWidth / 2
        for (i in 0 until 4) {
            val y = SudokuBoardGeometry.CELL_INSET - lineOffset + (mWidth * i * 3)
            canvas.drawLine(SudokuBoardGeometry.CELL_INSET - lineOffset, y, SudokuBoardGeometry.CELL_INSET + (mWidth * SudokuBoardGeometry.BOARD_SIZE) + lineOffset, y, mBlockLinePaint)
        }
        for (i in 0 until 4) {
            val x = SudokuBoardGeometry.CELL_INSET - lineOffset + (mWidth * i * 3)
            canvas.drawLine(x, SudokuBoardGeometry.CELL_INSET - lineOffset, x, SudokuBoardGeometry.CELL_INSET + (mWidth * SudokuBoardGeometry.BOARD_SIZE) + lineOffset, mBlockLinePaint)
        }

        /*
         * 绘制棋盘最外层白色边框，使棋盘从星空背景中突出。
         */
        val boardBorder = SudokuBoardGeometry.boardBorderRect(width.toFloat())
        mOuterRect.set(
            boardBorder.left.toInt(), boardBorder.top.toInt(),
            boardBorder.right.toInt(), boardBorder.bottom.toInt()
        )
        canvas.drawRect(mOuterRect, mOuterBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (mHasDone || mWrong) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x; val y = event.y
                // 只接受棋盘区域内的点击，边框和留白区域直接忽略。
                if (x < SudokuBoardGeometry.CELL_INSET || y < SudokuBoardGeometry.CELL_INSET || x >= SudokuBoardGeometry.CELL_INSET + mWidth * SudokuBoardGeometry.BOARD_SIZE || y >= SudokuBoardGeometry.CELL_INSET + mWidth * SudokuBoardGeometry.BOARD_SIZE) return false
                mCol = ((x - SudokuBoardGeometry.CELL_INSET) / mWidth).toInt().coerceIn(0, 8)
                mRow = ((y - SudokuBoardGeometry.CELL_INSET) / mWidth).toInt().coerceIn(0, 8)
                mBigBlock = getBigBlock(mRow, mCol)
                return true
            }
            MotionEvent.ACTION_UP -> {
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        mListener?.onTouch(mRow, mCol, mBigBlock)
        return true
    }

    fun getBigBlock(row: Int, col: Int): Int {
        if (row < 0 || row > 8 || col < 0 || col > 8) return 0
        // 宫号按 1..9 返回，与棋盘数据里的宫编号字段保持一致。
        return (row / 3) * 3 + (col / 3) + 1
    }
}
