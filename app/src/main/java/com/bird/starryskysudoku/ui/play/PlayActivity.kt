package com.bird.starryskysudoku.ui.play

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bird.starryskysudoku.BuildConfig
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.databinding.ActivityPlayBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.timer.CountdownTimerContract
import com.bird.starryskysudoku.ui.common.BaseLocalizedActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PlayActivity : BaseLocalizedActivity() {

    companion object {
        private const val DEBUG_COMPLETE_TOGGLE_TAP_COUNT = 5
        private const val WRONG_INPUT_DELAY_MILLIS = 200L
        private const val LOSE_DIALOG_DELAY_MILLIS = 1500L
        private const val WIN_DIALOG_DELAY_MILLIS = 2030L
        const val EXTRA_USERNAME = PlayRoute.EXTRA_USERNAME
    }

    private lateinit var mBinding: ActivityPlayBinding
    private lateinit var mViewModel: PlayViewModel
    private lateinit var mBroadView: BroadView
    private lateinit var mGameResultRecorder: GameResultRecorder
    private lateinit var mDialogController: PlayDialogController
    private lateinit var mCountdownCoordinator: CountdownCoordinator
    private lateinit var mInputController: PlayInputController
    private lateinit var mNavigationController: PlayNavigationController

    private lateinit var mPlayNum: TextView
    private lateinit var mTimeProgressBar: ProgressBar
    private lateinit var mRemainingMins: TextView
    private lateinit var mRemainingSecs: TextView
    private lateinit var mColon: TextView
    private val mNumbers = arrayOfNulls<TextView>(9)
    private lateinit var mRevoke: ImageView
    private lateinit var mTag: ImageView
    private lateinit var mPauseButton: ImageView
    private lateinit var mDebugCompleteButton: TextView

    private val mHandler = Handler(Looper.getMainLooper())
    private var mIsPaused = false
    private var mShouldStopCountdownOnDestroy = true
    private var mLevel = 1
    private var mCurrentUsername = LauncherSessionReader.GUEST_USERNAME
    private var mTagData = Array(9) { arrayOfNulls<TagData>(9) }
    private var mDebugCompleteToggleTapCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityPlayBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mLevel = PlayRoute.readLevel(intent)
        mCurrentUsername = PlayRoute.readUsername(intent)
            ?: LauncherSessionReader.readUsername(contentResolver)
        val maxNum = PlayRoute.MAX_LEVEL

        val db = DatabaseInitializer.getDatabase(this)
        mViewModel = ViewModelProvider(this, PlayViewModelFactory(db))[PlayViewModel::class.java]
        mGameResultRecorder = GameResultRecorder(db.gameResultDao())
        mCountdownCoordinator = CountdownCoordinator(
            mActivity = this,
            mGetRemainingSeconds = { mViewModel.getRemainingSeconds() },
            mGetLevel = { mLevel },
            mGetUsername = { mCurrentUsername },
            mCanStart = {
                mViewModel.mHasWon.value != true && mViewModel.mTimerFinished.value != true
            },
            mOnTick = { remainingSeconds -> mViewModel.updateRemainingSeconds(remainingSeconds) }
        )
        mDialogController = PlayDialogController(
            mActivity = this,
            mMaxLevel = maxNum,
            mGetLevel = { mLevel },
            mGetUsername = { mCurrentUsername },
            mRunAfterClearingHistory = { action -> clearHistoryAndRun(action) },
            mSetPaused = { paused -> mIsPaused = paused },
            mStartCountdownService = { mCountdownCoordinator.resume() },
            mPrepareForReplacementPlayActivity = {
                mShouldStopCountdownOnDestroy = false
            }
        )

        // 绑定所有 UI 控件
        mPlayNum = mBinding.playNum
        mPlayNum.text = mLevel.toString()
        mTimeProgressBar = mBinding.playTimeProgressbar
        mRemainingMins = mBinding.playTimeMin
        mRemainingSecs = mBinding.playTimeSec
        mColon = mBinding.playTimeColon
        mBroadView = mBinding.playBroad
        mNumbers[0] = mBinding.play1
        mNumbers[1] = mBinding.play2
        mNumbers[2] = mBinding.play3
        mNumbers[3] = mBinding.play4
        mNumbers[4] = mBinding.play5
        mNumbers[5] = mBinding.play6
        mNumbers[6] = mBinding.play7
        mNumbers[7] = mBinding.play8
        mNumbers[8] = mBinding.play9
        mRevoke = mBinding.playRevoke
        mTag = mBinding.playTag
        mPauseButton = mBinding.playPause
        mDebugCompleteButton = mBinding.playDebugComplete

        mInputController = PlayInputController(
            mScope = lifecycleScope,
            mViewModel = mViewModel,
            mBroadView = mBroadView,
            mNumbers = mNumbers,
            mRevoke = mRevoke,
            mTag = mTag,
            mTagData = mTagData,
            mGetLevel = { mLevel },
            mGetUsername = { mCurrentUsername },
            mOnPuzzleCompleted = { level ->
                saveGameResult(level, completed = true)
            }
        )

        mNavigationController = PlayNavigationController(
            mActivity = this,
            mViewModel = mViewModel,
            mPauseButton = mPauseButton,
            mDialogController = mDialogController,
            mCountdownCoordinator = mCountdownCoordinator,
            mSetPaused = { paused -> mIsPaused = paused },
            mIsPaused = { mIsPaused }
        )

        // 棋盘初始化：先禁用按钮，加载数据，再建立观察和触摸回调
        disableCellActions()
        mViewModel.initBoard(mLevel)
        observeBoard()
        initBoardTouchListener()

        // 游戏状态观察：剩余秒数、计时结束、通关、错误输入
        mTimeProgressBar.max = CountdownTimerContract.DEFAULT_TOTAL_SECONDS
        observeRemainingSeconds()
        observeGameState()

        mInputController.init()
        mNavigationController.init()
        if (BuildConfig.DEBUG) initDebugCompleteButton()
        mBinding.root.post {
            if (!mIsPaused) mCountdownCoordinator.start()
        }
    }

    override fun onStart() {
        super.onStart()
        mCountdownCoordinator.onStart()
    }

    override fun onPause() {
        super.onPause()
        mNavigationController.onPause()
    }

    override fun onStop() {
        mCountdownCoordinator.onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        mNavigationController.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        if (mShouldStopCountdownOnDestroy) mCountdownCoordinator.stop()
        mDialogController.hideAll()
    }

    // ——— 棋盘初始化和触摸 ————————————————————————————

    private fun observeBoard() {
        mViewModel.mBoard.observe(this) { board ->
            ensureTagData(board)
            mBroadView.initData(board)
            mBroadView.initTagData(mTagData)
            mBroadView.invalidate()
        }
    }

    private fun ensureTagData(board: Array<Array<BoardCell>>) {
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col].mValue == "0" && mTagData[row][col] == null) {
                    mTagData[row][col] = TagData()
                }
            }
        }
    }

    private fun initBoardTouchListener() {
        mBroadView.setListener(object : BroadView.Listener {
            override fun onTouch(row: Int, col: Int, block: Int) {
                mViewModel.setCurrentPosition(row, col, block)
                mViewModel.selectCell(row, col)
                val cell = mViewModel.mBoard.value?.get(row)?.get(col) ?: return
                refreshCellActionAlpha(cell, row, col)
                mBroadView.invalidate()
                PlayMusic.getInstance().playButtonTap()
            }
        })
    }

    private fun refreshCellActionAlpha(cell: BoardCell, row: Int, col: Int) {
        when {
            cell.mType == BoardCell.PROBLEM -> {
                setNumberEnabled(false)
                setButtonEnabled(mTag, false)
            }
            cell.mValue != "0" -> {
                setNumberEnabled(true)
                setButtonEnabled(mTag, false)
            }
            else -> {
                setButtonEnabled(mTag, true)
                if (mViewModel.isTagMode()) {
                    refreshTagNumberAlpha(row, col)
                } else {
                    setNumberEnabled(true)
                }
            }
        }
        setButtonEnabled(mRevoke, true)
    }

    private fun refreshTagNumberAlpha(row: Int, col: Int) {
        val tagData = mTagData[row][col]
        for (index in 0 until 9) {
            val tagged = tagData != null && tagData.haveTag((index + 1).toString())
            mNumbers[index]?.isEnabled = true
            mNumbers[index]?.alpha = if (tagged) BoardCell.DIM_ALPHA else 1f
        }
    }

    private fun disableCellActions() {
        setNumberEnabled(false)
        setButtonEnabled(mTag, false)
        setButtonEnabled(mRevoke, false)
    }

    private fun setNumberEnabled(enabled: Boolean) {
        for (number in mNumbers) {
            number?.isEnabled = enabled
            number?.alpha = if (enabled) 1f else BoardCell.DIM_ALPHA
        }
    }

    private fun setButtonEnabled(view: ImageView, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else BoardCell.DIM_ALPHA
    }

    // ——— 游戏状态观察和响应 ————————————————————————————

    private fun observeRemainingSeconds() {
        mViewModel.mRemainingSeconds.observe(this) { seconds ->
            val min = seconds / 60
            val sec = seconds % 60
            mTimeProgressBar.progress = seconds
            mRemainingMins.text = String.format(Locale.ROOT, "%02d", min)
            mRemainingSecs.text = String.format(Locale.ROOT, "%02d", sec)
            refreshCountdownColor(seconds)
        }
    }

    private fun refreshCountdownColor(seconds: Int) {
        if (seconds in 1..10) {
            setCountdownTextColor(R.color.red)
            PlayMusic.getInstance().playTimesUp()
        } else {
            setCountdownTextColor(R.color.white)
        }
    }

    private fun setCountdownTextColor(colorRes: Int) {
        val color = ContextCompat.getColor(this, colorRes)
        mRemainingMins.setTextColor(color)
        mRemainingSecs.setTextColor(color)
        mColon.setTextColor(color)
    }

    private fun observeGameState() {
        mViewModel.mTimerFinished.observe(this) { finished ->
            if (finished) showLoseState()
        }

        mViewModel.mHasWon.observe(this) { won ->
            if (won && !mIsPaused) {
                mCountdownCoordinator.stop()
                PlayMusic.getInstance().stopTimesUp()
                PlayMusic.getInstance().playWinning()
                playWinAnimation()
            }
        }

        mViewModel.mIsWrong.observe(this) { wrong ->
            if (wrong) showWrongInputState()
        }
    }

    private fun playWinAnimation() {
        val board = mViewModel.mBoard.value ?: return
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                board[row][col].mStatus = BoardCell.SELECT_NONE
            }
        }
        mBroadView.overDone(board)
        mBroadView.invalidate()

        for (line in 8 downTo 0) {
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofInt(mBroadView, "TextSize", 80, 96, 80),
                    ObjectAnimator.ofInt(mBroadView, "Line", line, line)
                )
                interpolator = LinearInterpolator()
                duration = 400
                startDelay = ((8 - line) * 200).toLong()
                start()
            }
        }

        mHandler.postDelayed({
            mDialogController.showWinDialogWithStarAnimation()
        }, WIN_DIALOG_DELAY_MILLIS)
    }

    private fun showLoseState() {
        mBroadView.setWrong(true)
        PlayMusic.getInstance().stopTimesUp()
        PlayMusic.getInstance().playLosing()
        lifecycleScope.launch {
            saveGameResult(mLevel, false)
        }
        mHandler.postDelayed({
            mDialogController.showLoseDialog()
        }, LOSE_DIALOG_DELAY_MILLIS)
    }

    private fun showWrongInputState() {
        mBroadView.setWrong(true)
        PlayMusic.getInstance().playInputWrong()
        mViewModel.setCanInsert(false)
        mHandler.postDelayed({
            mViewModel.revertWrongInput(mViewModel.getCurrentRow(), mViewModel.getCurrentCol())
            mViewModel.mBoard.value?.let { board ->
                mBroadView.initData(board)
                mBroadView.invalidate()
            }
            mBroadView.setWrong(false)
            mViewModel.setCanInsert(true)
            // 回退后格子恢复为空格，标记按钮需要重新启用
            val canUseTag = mViewModel.currentCellIsEmpty()
            mTag.isEnabled = canUseTag
            mTag.alpha = if (canUseTag) 1f else BoardCell.DIM_ALPHA
            mRevoke.alpha = 1f
        }, WRONG_INPUT_DELAY_MILLIS)
    }

    // ——— 战绩保存 ————————————————————————————————————

    private suspend fun saveGameResult(levelNum: Int, completed: Boolean) {
        if (!mViewModel.markGameResultRecordStarted(levelNum, completed)) return
        val saved = withContext(Dispatchers.IO) {
            mGameResultRecorder.save(
                level = levelNum,
                remainingSeconds = mViewModel.getRemainingSeconds(),
                completed = completed,
                username = mCurrentUsername
            )
        }
        if (!saved) {
            mViewModel.clearGameResultRecordMark(levelNum, completed)
        }
    }

    private fun clearHistoryAndRun(action: () -> Unit) {
        lifecycleScope.launch {
            mViewModel.clearHistory()
            action()
        }
    }

    // ——— 调试按钮 ————————————————————————————————————

    private fun initDebugCompleteButton() {
        mDebugCompleteButton.visibility = View.GONE
        val toggleDebugComplete = View.OnClickListener {
            mDebugCompleteToggleTapCount++
            if (mDebugCompleteToggleTapCount < DEBUG_COMPLETE_TOGGLE_TAP_COUNT) return@OnClickListener
            mDebugCompleteToggleTapCount = 0
            mDebugCompleteButton.visibility = if (mDebugCompleteButton.visibility == View.VISIBLE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        mBinding.playTopBar.setOnClickListener(toggleDebugComplete)
        mBinding.playStarLabel.setOnClickListener(toggleDebugComplete)
        mPlayNum.setOnClickListener(toggleDebugComplete)
        mDebugCompleteButton.setOnClickListener {
            if (mViewModel.mHasWon.value == true) return@setOnClickListener
            lifecycleScope.launch {
                mViewModel.updatePassStatus(mCurrentUsername, mLevel, mLevel + 1)
                saveGameResult(mLevel, completed = true)
                mViewModel.markWonForDebug()
            }
        }
    }
}
