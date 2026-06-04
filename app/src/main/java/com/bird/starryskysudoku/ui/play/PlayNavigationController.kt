package com.bird.starryskysudoku.ui.play

import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bird.starryskysudoku.media.PlayMusic

class PlayNavigationController(
    private val mActivity: AppCompatActivity,
    private val mViewModel: PlayViewModel,
    private val mPauseButton: ImageView,
    private val mDialogController: PlayDialogController,
    private val mCountdownCoordinator: CountdownCoordinator,
    private val mSetPaused: (Boolean) -> Unit,
    private val mIsPaused: () -> Boolean
) {
    fun init() {
        // 页面导航层只处理暂停入口和生命周期恢复，不参与棋盘数据或倒计时计算。
        initPauseButton()
        initBackHandler()
    }

    fun onPause() {
        // 页面离开前先收掉持续型提示音，避免回到后台后还残留终局或超时音效。
        PlayMusic.getInstance().stopTimesUp()
        if (mViewModel.mHasWon.value == true) {
            PlayMusic.getInstance().stopWinning()
        }
        pauseActiveGameForBackground()
    }

    fun onResume() {
        // 恢复时只补齐终局或已暂停弹窗，不在生命周期回调中启停倒计时服务。
        if (mViewModel.mHasWon.value == true) {
            mDialogController.showWinDialog()
        } else if (mViewModel.mTimerFinished.value == true) {
            mDialogController.showLoseDialog()
        } else if (mIsPaused() && !mDialogController.isPauseDialogShowing()) {
            mDialogController.showPauseDialog()
        }
    }

    private fun pauseActiveGameForBackground() {
        if (mViewModel.mHasWon.value == true) return
        if (mViewModel.mTimerFinished.value == true) return
        if (mIsPaused()) return
        mSetPaused(true)
        // 回到后台时暂停计时但保留前台服务通知
        mCountdownCoordinator.pause()
    }

    private fun initPauseButton() {
        mPauseButton.setOnClickListener {
            // 已通关后暂停按钮不再生效，避免胜利弹窗和暂停弹窗同时出现。
            if (mViewModel.mHasWon.value == true) return@setOnClickListener
            showPauseDialog()
        }
    }

    private fun initBackHandler() {
        mActivity.onBackPressedDispatcher.addCallback(mActivity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 棋盘页返回键不直接退出，统一走暂停弹窗，避免误触丢失当前局进度。
                showPauseDialog()
            }
        })
    }

    private fun showPauseDialog() {
        PlayMusic.getInstance().stopTimesUp()
        mSetPaused(true)
        mCountdownCoordinator.pause()
        mDialogController.showPauseDialog()
    }
}
