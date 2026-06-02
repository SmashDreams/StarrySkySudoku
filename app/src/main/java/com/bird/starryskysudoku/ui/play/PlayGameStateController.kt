package com.bird.starryskysudoku.ui.play

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.timer.CountdownTimerContract
import kotlinx.coroutines.launch
import java.util.Locale

class PlayGameStateController(
    private val mContext: Context,
    private val mLifecycleOwner: LifecycleOwner,
    private val mScope: LifecycleCoroutineScope,
    private val mViewModel: PlayViewModel,
    private val mBroadView: BroadView,
    private val mTimeProgressBar: ProgressBar,
    private val mRemainingMins: TextView,
    private val mRemainingSecs: TextView,
    private val mColon: TextView,
    private val mDialogController: PlayDialogController,
    private val mCountdownCoordinator: CountdownCoordinator,
    private val mIsPaused: () -> Boolean,
    private val mGetLevel: () -> Int,
    private val mSaveGameResult: suspend (Int, Boolean) -> Unit
) {
    private val mHandler = Handler(Looper.getMainLooper())

    fun init() {
        // 进度条最大值直接复用整局秒数，后续每秒更新当前剩余时间即可。
        mTimeProgressBar.max = CountdownTimerContract.DEFAULT_TOTAL_SECONDS
        observeRemainingSeconds()
        observeGameState()
    }

    fun clearCallbacks() {
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun observeRemainingSeconds() {
        mViewModel.mRemainingSeconds.observe(mLifecycleOwner) { seconds ->
            val min = seconds / 60
            val sec = seconds % 60
            // 文本和进度条都只依赖同一个剩余秒数源，避免界面内部出现不同步。
            mTimeProgressBar.progress = seconds
            mRemainingMins.text = String.format(Locale.ROOT, "%02d", min)
            mRemainingSecs.text = String.format(Locale.ROOT, "%02d", sec)
            refreshCountdownColor(seconds)
        }
    }

    private fun refreshCountdownColor(seconds: Int) {
        // 最后十秒统一切红并播放提示音，强化时间压力。
        if (seconds in 1..10) {
            setCountdownTextColor(R.color.red)
            PlayMusic.getInstance().playTimesUp()
        } else {
            setCountdownTextColor(R.color.white)
        }
    }

    private fun setCountdownTextColor(colorRes: Int) {
        val color = ContextCompat.getColor(mContext, colorRes)
        mRemainingMins.setTextColor(color)
        mRemainingSecs.setTextColor(color)
        mColon.setTextColor(color)
    }

    private fun observeGameState() {
        mViewModel.mTimerFinished.observe(mLifecycleOwner) { finished ->
            if (finished) showLoseState()
        }

        mViewModel.mHasWon.observe(mLifecycleOwner) { won ->
            // 只有未暂停状态下的真实通关才触发动画，避免恢复页面时重复播放。
            if (won && !mIsPaused()) {
                mCountdownCoordinator.stop()
                PlayMusic.getInstance().stopTimesUp()
                PlayMusic.getInstance().playWinning()
                playWinAnimation()
            }
        }

        mViewModel.mIsWrong.observe(mLifecycleOwner) { wrong ->
            if (wrong) showWrongInputState()
        }
    }

    private fun playWinAnimation() {
        val board = mViewModel.mBoard.value ?: return
        // 通关动画前先清掉高亮，让逐行闪烁只保留数字本身。
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                board[row][col].mStatus = PlayViewModel.SELECT_NONE
            }
        }
        mBroadView.overDone(board)
        mBroadView.invalidate()

        val winningAnims = Array(9) { AnimatorSet() }
        for (line in 8 downTo 0) {
            // 从底部到顶部逐行闪动，和原项目视觉节奏保持一致。
            winningAnims[line] = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofInt(mBroadView, "TextSize", 80, 100, 80),
                    ObjectAnimator.ofInt(mBroadView, "TextAlpha", 255, 0, 255),
                    ObjectAnimator.ofInt(mBroadView, "Line", line, line)
                )
                interpolator = LinearInterpolator()
                duration = 500
                startDelay = ((8 - line) * 200).toLong()
                start()
            }
        }

        mHandler.postDelayed({
            mDialogController.showWinDialogWithStarAnimation()
        }, WIN_DIALOG_DELAY_MILLIS)
    }

    private fun showLoseState() {
        // 失败态先冻结棋盘和音效，再异步落库，最后延迟弹出失败弹窗。
        mBroadView.setWrong(true)
        PlayMusic.getInstance().stopTimesUp()
        PlayMusic.getInstance().playLosing()
        mScope.launch {
            mSaveGameResult(mGetLevel(), false)
        }
        mHandler.postDelayed({
            mDialogController.showLoseDialog()
        }, LOSE_DIALOG_DELAY_MILLIS)
    }

    private fun showWrongInputState() {
        mBroadView.setWrong(true)
        PlayMusic.getInstance().playInputWrong()
        mViewModel.setCanInsert(false)
        // 错误提示期间临时冻结输入，等恢复原值后再放开继续操作。
        mHandler.postDelayed({
            mViewModel.revertWrongInput(mViewModel.getCurrentRow(), mViewModel.getCurrentCol())
            mBroadView.initData(mViewModel.mBoard.value!!)
            mBroadView.invalidate()
            mBroadView.setWrong(false)
            mViewModel.setCanInsert(true)
        }, WRONG_INPUT_DELAY_MILLIS)
    }

    private companion object {
        private const val WRONG_INPUT_DELAY_MILLIS = 200L
        private const val LOSE_DIALOG_DELAY_MILLIS = 1500L
        private const val WIN_DIALOG_DELAY_MILLIS = 2030L
    }
}
