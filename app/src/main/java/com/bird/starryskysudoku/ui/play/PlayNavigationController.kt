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
        initPauseButton()
        initBackHandler()
    }

    fun onPause() {
        PlayMusic.getInstance().stopTimesUp()
        if (mViewModel.mHasWon.value == true) {
            PlayMusic.getInstance().stopWinning()
        }
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

    private fun initPauseButton() {
        mPauseButton.setOnClickListener {
            if (mViewModel.mHasWon.value == true) return@setOnClickListener
            showPauseDialog()
        }
    }

    private fun initBackHandler() {
        mActivity.onBackPressedDispatcher.addCallback(mActivity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showPauseDialog()
            }
        })
    }

    private fun showPauseDialog() {
        // 返回键和暂停按钮共用同一套暂停入口，确保倒计时停止逻辑一致。
        PlayMusic.getInstance().playDialogShow()
        PlayMusic.getInstance().stopTimesUp()
        mSetPaused(true)
        mCountdownCoordinator.stop()
        mDialogController.showPauseDialog()
    }
}
