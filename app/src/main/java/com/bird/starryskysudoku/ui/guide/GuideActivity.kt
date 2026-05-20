package com.bird.starryskysudoku.ui.guide

import android.content.Intent
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.map.MapActivity
import com.bird.starryskysudoku.ui.play.BroadView
import com.bird.starryskysudoku.ui.play.SudokuBoardGeometry
import kotlinx.coroutines.launch

class GuideActivity : AppCompatActivity() {

    private val mSteps = GuideStep.entries.toTypedArray()
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
        setContentView(R.layout.activity_guidepage)

        PlayMusic.getInstance().playBGM()

        bindViews()
        initTouch()
        initBackHandler()
        renderStep()
        loadLevelOneBoard()
    }

    private fun bindViews() {
        mRoot = findViewById(R.id.guide_root)
        mDescription = findViewById(R.id.guide_description)
        mBoard = findViewById(R.id.guide_board)
        mTimerFocus = findViewById(R.id.guide_timer_focus)
        mTimeProgress = findViewById(R.id.guide_time_progressbar)
        mNumberPanel = findViewById(R.id.guide_number_panel)
        mSpotlight = findViewById(R.id.guide_spotlight)
        mHint = findViewById(R.id.guide_hint)
        mNumberKeys = arrayOf(
            findViewById(R.id.guide_num_1),
            findViewById(R.id.guide_num_2),
            findViewById(R.id.guide_num_3),
            findViewById(R.id.guide_num_4),
            findViewById(R.id.guide_num_5),
            findViewById(R.id.guide_num_6),
            findViewById(R.id.guide_num_7),
            findViewById(R.id.guide_num_8),
            findViewById(R.id.guide_num_9)
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
            if (values.size == 81) {
                mLevelOneValues = values
                renderStep()
            }
        }
    }

    private fun renderStep() {
        val step = mSteps[mCurrentStep]
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
        for (index in mNumberKeys.indices) {
            val key = mNumberKeys[index]
            val isDemoNumber = step == GuideStep.ENTER_NUMBER && index == DEMO_NUMBER_INDEX
            key.alpha = if (isDemoNumber) 1f else 0.72f
            key.scaleX = 1f
            key.scaleY = 1f
        }
    }

    private fun renderTimer(step: GuideStep) {
        mTimeProgress.progress = if (step == GuideStep.TIMER) 455 else 600
    }

    private fun renderSpotlight(step: GuideStep) {
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
        val row = demoCellIndex().first
        val col = demoCellIndex().second
        return GuideRuleHighlightCells.regionsFor(row, col).map { region ->
            boardAreaRect(
                startRow = region.startRow,
                startCol = region.startCol,
                rowSpan = region.rowSpan,
                colSpan = region.colSpan,
                leftTopPadding = 3f,
                rightBottomPadding = 2f
            )
        }
    }

    private fun boardCellRect(): RectF {
        val (row, col) = demoCellIndex()
        return cellAreaRect(row, col, 1f)
    }

    private fun boardBorderRect(extraPadding: Float): RectF {
        val origin = viewRect(mBoard, 0f)
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
        rect.inset(NUMBER_KEY_HORIZONTAL_INSET, NUMBER_KEY_VERTICAL_INSET)
        return rect
    }

    private fun SudokuBoardGeometry.BoardRect.toRectF(): RectF {
        return RectF(left, top, right, bottom)
    }

    private fun demoCellIndex(): Pair<Int, Int> {
        var bestCell: Pair<Int, Int>? = null
        var bestDistance = Int.MAX_VALUE
        for (index in mLevelOneValues.indices) {
            if (mLevelOneValues[index] == 0) {
                val row = index / 9
                val col = index % 9
                val distance = kotlin.math.abs(row - CENTER_INDEX) + kotlin.math.abs(col - CENTER_INDEX)
                if (distance < bestDistance) {
                    bestDistance = distance
                    bestCell = row to col
                }
            }
        }
        return bestCell ?: (CENTER_INDEX to CENTER_INDEX)
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
        getSharedPreferences("firstcome", MODE_PRIVATE).edit {
            putBoolean("first", false)
        }
        startActivityWithTransition(
            Intent(this, MapActivity::class.java)
                .putExtra(MapActivity.EXTRA_FLASH_HOME, false),
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
                finishAffinity()
            }
        })
    }

    override fun onResume() { super.onResume(); PlayMusic.getInstance().playBGM() }
    override fun onPause() { super.onPause(); PlayMusic.getInstance().stopBGM() }

    companion object {
        private const val FIRST_LEVEL = 1
        private const val DEMO_NUMBER_INDEX = 6
        private const val CENTER_INDEX = 4
        private const val CELL_RIGHT_BOTTOM_ADJUST = 2f
        private const val NUMBER_KEY_HORIZONTAL_INSET = 2f
        private const val NUMBER_KEY_VERTICAL_INSET = 11f
    }
}
