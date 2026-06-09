package com.bird.starryskysudoku.ui.guide

import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.bird.starryskysudoku.ui.common.BaseLocalizedActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.databinding.ActivityGuidepageBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.map.MapRoute
import com.bird.starryskysudoku.ui.play.BroadView
import com.bird.starryskysudoku.ui.play.SudokuBoardGeometry
import kotlinx.coroutines.launch

class GuideActivity : BaseLocalizedActivity() {

    private val mSteps = GuideStep.entries.toTypedArray()
    private lateinit var mBinding: ActivityGuidepageBinding
    private lateinit var mRoot: View
    private lateinit var mDescription: TextView
    private lateinit var mBoard: BroadView
    private lateinit var mTimerFocus: View
    private lateinit var mTimeProgress: ProgressBar
    private lateinit var mNumberPanel: View
    private lateinit var mSpotlight: GuideSpotlightView
    private lateinit var mHint: TextView
    private lateinit var mNumberKeys: Array<TextView>
    private var mCurrentStep = 0
    private var mLevelOneValues = List(81) { 0 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityGuidepageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        bindViews()
        initTouch()
        initBackHandler()
        loadLevelOneBoard()
    }

    private fun bindViews() {
        mRoot = mBinding.guideRoot
        mDescription = mBinding.guideDescription
        mBoard = mBinding.guideBoard
        mTimerFocus = mBinding.guideTimerFocus
        mTimeProgress = mBinding.guideTimeProgressbar
        mNumberPanel = mBinding.guideNumberPanel
        mSpotlight = mBinding.guideSpotlight
        mHint = mBinding.guideHint
        mNumberKeys = arrayOf(
            mBinding.guideNum1,
            mBinding.guideNum2,
            mBinding.guideNum3,
            mBinding.guideNum4,
            mBinding.guideNum5,
            mBinding.guideNum6,
            mBinding.guideNum7,
            mBinding.guideNum8,
            mBinding.guideNum9
        )
    }

    private fun initTouch() {
        val advance = View.OnClickListener { goNext() }
        mRoot.setOnClickListener(advance)
        mBoard.setOnClickListener(advance)
        mTimerFocus.setOnClickListener(advance)
        mNumberPanel.setOnClickListener(advance)
    }

    private fun loadLevelOneBoard() {
        val db = DatabaseInitializer.getDatabase(this)
        lifecycleScope.launch {
            val values = db.problemDao().getValuesForLevel(FIRST_LEVEL)
            mLevelOneValues = values
            renderStep()
        }
    }

    private fun renderStep() {
        val step = mSteps[mCurrentStep]
        // 每一步都重建演示状态，保证从任意步骤返回时界面表现一致。
        mDescription.text = descriptionText(step)
        mHint.setText(if (step == GuideStep.GOOD_LUCK) R.string.guide_start else R.string.tap)

        val board = GuideBoardFactory.createBoard(mLevelOneValues, step)
        mBoard.initData(board)
        mBoard.invalidate()
        renderNumberKeys(step)
        renderTimer(step)
        mRoot.post { renderSpotlight(step) }
    }

    private fun renderNumberKeys(step: GuideStep) {
        for (key in mNumberKeys) key.alpha = 0.72f
        if (step == GuideStep.ENTER_NUMBER) mNumberKeys[DEMO_NUMBER_INDEX].alpha = 1f
    }

    private fun renderTimer(step: GuideStep) {
        mTimeProgress.progress = if (step == GuideStep.TIMER) 455 else 600
    }

    private fun renderSpotlight(step: GuideStep) {
        // 聚焦框只负责提示当前步骤重点，棋盘高亮由引导棋盘工厂负责。
        when (step) {
            GuideStep.WELCOME -> mSpotlight.setHighlights(listOf(boardBorderRect(5f)), dimmed = true)
            GuideStep.RULE_UNIQUE -> mSpotlight.setHighlights(ruleHighlights(), dimmed = true)
            GuideStep.SELECT_CELL -> mSpotlight.setHighlights(listOf(boardCellRect()), dimmed = true)
            GuideStep.ENTER_NUMBER -> mSpotlight.setHighlights(listOf(numberKeyRect(mNumberKeys[DEMO_NUMBER_INDEX])), dimmed = true)
            GuideStep.TIMER -> mSpotlight.setHighlights(listOf(viewRect(mTimerFocus, 7f)), dimmed = true)
            GuideStep.GOOD_LUCK -> mSpotlight.setHighlights(emptyList(), dimmed = true)
        }
    }

    private fun ruleHighlights(): List<RectF> {
        val row = CENTER_ROW
        val col = CENTER_COL
        // 规则演示不是只框住一个格子，而是把同行、同列和同宫拆成多个聚焦区域。
        return GuideRuleHighlightCells.regionsFor(row, col).map { region ->
            boardAreaRect(
                startRow = region.mStartRow,
                startCol = region.mStartCol,
                rowSpan = region.mRowSpan,
                colSpan = region.mColSpan,
                leftTopPadding = 3f,
                rightBottomPadding = 2f
            )
        }
    }

    private fun boardCellRect(): RectF {
        return cellAreaRect(CENTER_ROW, CENTER_COL, 1f)
    }

    private fun boardBorderRect(extraPadding: Float): RectF {
        val origin = viewRect(mBoard, 0f)
        // 棋盘外框沿用统一几何工具，保证引导高亮和真实棋盘边界完全重合。
        return SudokuBoardGeometry.boardBorderRect(
            width = mBoard.width.toFloat(),
            left = origin.left,
            top = origin.top,
            padding = extraPadding
        ).toRectF()
    }

    private fun boardAreaRect(
        startRow: Int,
        startCol: Int,
        rowSpan: Int,
        colSpan: Int,
        leftTopPadding: Float,
        rightBottomPadding: Float
    ): RectF {
        val origin = viewRect(mBoard, 0f)
        // 这里先按设计坐标求出区域，再补左右下角微调，让虚线框更贴合素材边界。
        val rect = SudokuBoardGeometry.boardRegionRect(
            width = mBoard.width.toFloat(),
            startRow = startRow,
            startCol = startCol,
            rowSpan = rowSpan,
            colSpan = colSpan,
            left = origin.left,
            top = origin.top,
            padding = 0f
        ).toRectF()
        rect.left -= leftTopPadding
        rect.top -= leftTopPadding
        rect.right += rightBottomPadding
        rect.bottom += rightBottomPadding
        return rect
    }

    private fun cellAreaRect(row: Int, col: Int, extraPadding: Float): RectF {
        val origin = viewRect(mBoard, 0f)
        return SudokuBoardGeometry.cellRect(
            width = mBoard.width.toFloat(),
            row = row,
            col = col,
            left = origin.left,
            top = origin.top,
            padding = extraPadding,
            rightBottomAdjust = CELL_RIGHT_BOTTOM_ADJUST
        ).toRectF()
    }

    private fun viewRect(view: View, extraPadding: Float): RectF {
        val rootLocation = IntArray(2)
        val viewLocation = IntArray(2)
        // 聚焦层和内容层共用根布局坐标系，避免不同父容器带来额外偏移。
        mRoot.getLocationOnScreen(rootLocation)
        view.getLocationOnScreen(viewLocation)
        val left = (viewLocation[0] - rootLocation[0]).toFloat()
        val top = (viewLocation[1] - rootLocation[1]).toFloat()
        return RectF(
            left - extraPadding,
            top - extraPadding,
            left + view.width + extraPadding,
            top + view.height + extraPadding
        )
    }

    private fun numberKeyRect(view: View): RectF {
        val rect = viewRect(view, 0f)
        // 数字键素材四周自带留白，这里再向内收一点让聚焦框更贴近按钮主体。
        rect.inset(NUMBER_KEY_HORIZONTAL_INSET, NUMBER_KEY_VERTICAL_INSET)
        return rect
    }

    private fun SudokuBoardGeometry.BoardRect.toRectF(): RectF {
        return RectF(mLeft, mTop, mRight, mBottom)
    }

    private fun goNext() {
        PlayMusic.getInstance().playButtonTap()
        if (mCurrentStep < mSteps.lastIndex) {
            mCurrentStep++
            renderStep()
        } else {
            finishGuide()
        }
    }

    private fun finishGuide() {
        // 首次引导只展示一次，完成后直接进入地图页。
        getSharedPreferences("firstcome", MODE_PRIVATE).edit {
            putBoolean("first", false)
        }
        startActivityWithTransition(
            MapRoute.create(this),
            R.anim.playpage_show,
            R.anim.playpage_hide
        )
        finish()
    }

    private fun descriptionText(step: GuideStep): String {
        return when (step) {
            GuideStep.WELCOME -> "${getString(R.string.guide_1_1)}\n${getString(R.string.guide_1_2)}"
            GuideStep.RULE_UNIQUE -> getString(R.string.guide_2)
            GuideStep.SELECT_CELL -> getString(R.string.guide_3)
            GuideStep.ENTER_NUMBER -> getString(R.string.guide_4)
            GuideStep.TIMER -> getString(R.string.guide_5)
            GuideStep.GOOD_LUCK -> getString(R.string.guide_6)
        }
    }

    private fun initBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 本页返回键直接退出应用，避免回到没有完成初始化的入口页。
                finishAffinity()
            }
        })
    }


    companion object {
        private const val FIRST_LEVEL = 1
        private const val DEMO_NUMBER_INDEX = 6
        private const val CENTER_ROW = 4
        private const val CENTER_COL = 4
        private const val CELL_RIGHT_BOTTOM_ADJUST = 2f
        private const val NUMBER_KEY_HORIZONTAL_INSET = 2f
        private const val NUMBER_KEY_VERTICAL_INSET = 11f
    }
}
