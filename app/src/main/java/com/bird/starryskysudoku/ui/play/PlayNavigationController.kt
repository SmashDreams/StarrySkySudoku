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
        mSetPaused(true)
        PlayMusic.getInstance().stopBGM()
        PlayMusic.getInstance().stopTimesUp()
        mCountdownCoordinator.stop()
        if (mViewModel.mHasWon.value == true) {
            PlayMusic.getInstance().stopWinning()
        }
    }

    fun onResume() {
        PlayMusic.getInstance().playBGM()
        if (mViewModel.mHasWon.value == true) {
            mDialogController.showWinDialog()
        } else if (mViewModel.mTimerFinished.value == true) {
            mDialogController.showLoseDialog()
        } else if (mIsPaused() && !mDialogController.isPauseDialogShowing()) {
            mDialogController.showPauseDialog()
        } else if (!mIsPaused()) {
            mCountdownCoordinator.start()
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
        PlayMusic.getInstance().playDialogShow()
        PlayMusic.getInstance().stopTimesUp()
        mSetPaused(true)
        mCountdownCoordinator.stop()
        mDialogController.showPauseDialog()
    }
}
