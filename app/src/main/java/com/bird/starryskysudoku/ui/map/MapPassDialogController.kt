package com.bird.starryskysudoku.ui.map

import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.repository.PassStatus
import com.bird.starryskysudoku.databinding.DialogPasscheckBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager

class MapPassDialogController(
    private val mActivity: AppCompatActivity,
    private val mLayoutInflater: LayoutInflater,
    private val mViewModel: MapViewModel,
    private val mHandler: Handler,
    private val mGetUsername: () -> String,
    private val mCreatePlayIntent: (String) -> Intent,
    private val mOpenPlayPage: (Intent, Boolean) -> Unit,
    private val mParseLevel: (String?) -> Int?,
    private val mScrollAfterCompletedLevel: (Int) -> Unit,
    private val mSetMapInteractionEnabled: (Boolean) -> Unit
) {
    private var mNextNum: String? = null
    private var mLoseNum: String? = null
    private var mCompletedNum: String? = null
    private var mLightStars = 0
    private var mReturnAnchorPosition: Int? = null
    private var mReturnAnchorOffsetPx: Int? = null
    private var mPendingShowDialog: Runnable? = null
    private var mActiveDialog: MyDialog? = null
    private var mInteractionLocked = false

    val lightStars: Int
        get() = mLightStars

    val hasPendingWinNavigation: Boolean
        get() = mCompletedNum != null

    val pendingCompletedLevel: Int?
        get() = mCompletedNum?.toIntOrNull()

    val hasPendingReturnAnchor: Boolean
        get() = mReturnAnchorPosition != null && mReturnAnchorOffsetPx != null

    val pendingReturnAnchorPosition: Int?
        get() = mReturnAnchorPosition

    val pendingReturnAnchorOffsetPx: Int?
        get() = mReturnAnchorOffsetPx

    fun clearPendingReturnAnchor() {
        mReturnAnchorPosition = null
        mReturnAnchorOffsetPx = null
    }

    fun consumeNavigationExtras(intent: Intent) {
        /*
         * 胜利/失败返回地图时会携带关卡信息，这里统一消费并清理，避免页面恢复后重复弹窗。
         */
        mNextNum = null
        mLoseNum = null
        mCompletedNum = null
        mReturnAnchorPosition = null
        mReturnAnchorOffsetPx = null
        mCompletedNum = intent.getStringExtra(MapRoute.EXTRA_COMPLETED_LEVEL)
            ?.takeIf { mParseLevel(it) != null }
        mNextNum = intent.getStringExtra(MapRoute.EXTRA_NEXT_LEVEL)?.takeIf { mParseLevel(it) != null }
        mLoseNum = intent.getStringExtra(MapRoute.EXTRA_LOSE_LEVEL)?.takeIf { mParseLevel(it) != null }
        if (
            intent.hasExtra(MapRoute.EXTRA_RETURN_ANCHOR_POSITION) &&
            intent.hasExtra(MapRoute.EXTRA_RETURN_ANCHOR_OFFSET)
        ) {
            mReturnAnchorPosition = intent.getIntExtra(MapRoute.EXTRA_RETURN_ANCHOR_POSITION, -1)
                .takeIf { it >= 0 }
            mReturnAnchorOffsetPx = intent.getIntExtra(MapRoute.EXTRA_RETURN_ANCHOR_OFFSET, 0)
        }
        intent.removeExtra(MapRoute.EXTRA_ROLL_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_COMPLETED_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_NEXT_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_LOSE_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_RETURN_ANCHOR_POSITION)
        intent.removeExtra(MapRoute.EXTRA_RETURN_ANCHOR_OFFSET)

        mCompletedNum?.toIntOrNull()?.let { completedLevel ->
            setMapInteractionEnabled(false)
            mLightStars = completedLevel
            val showNextLevel = mNextNum != null
            mHandler.postDelayed({
                mScrollAfterCompletedLevel(completedLevel)
            }, MAP_COMPLETION_SCROLL_DELAY_MILLIS)
            if (showNextLevel) {
                mNextNum?.let { handleCheckNum(it, WIN_NEXT_DIALOG_DELAY_MILLIS) }
            } else {
                mHandler.postDelayed({
                    setMapInteractionEnabled(true)
                }, WIN_NEXT_DIALOG_DELAY_MILLIS)
            }
        }
        if (mCompletedNum == null) {
            mLoseNum?.let { handleCheckNum(it, LOSE_DIALOG_DELAY_MILLIS) }
        }
    }

    fun openForEntity(entity: MapEntity) {
        if (mInteractionLocked) return
        dismissActivePassDialog()
        when (entity.mStatus) {
            PassStatus.TODO -> openPassCheck(entity.mPassNum.toString(), delayMillis = 0L)
            PassStatus.COMPLETED -> openRetryCheck(entity.mPassNum.toString(), delayMillis = 0L)
        }
    }

    private fun handleCheckNum(num: String, delayMillis: Long) {
        val passNum = mParseLevel(num) ?: return
        mViewModel.getPassStatus(mGetUsername(), passNum) { status ->
            when (status) {
                PassStatus.TODO -> openPassCheck(passNum.toString(), delayMillis)
                PassStatus.COMPLETED -> openRetryCheck(passNum.toString(), delayMillis)
            }
        }
    }

    private fun openPassCheck(checkNum: String, delayMillis: Long) {
        val passCheckBinding = DialogPasscheckBinding.inflate(mLayoutInflater)
        val dialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_passcheck, passCheckBinding.root)
            .apply {
                passCheckBinding.passcheckNum.text = checkNum
                passCheckBinding.passcheckPasstimes.text = "0"
                passCheckBinding.passcheckStar.setImageResource(R.drawable.star_empty)

                passCheckBinding.passcheckClose.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    hidePassDialog(this)
                }

                passCheckBinding.passcheckStart.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    hidePassDialog(this)
                    mOpenPlayPage(mCreatePlayIntent(checkNum), true)
                }
            }

        showPassDialog(dialog, delayMillis)
    }

    private fun openRetryCheck(checkNum: String, delayMillis: Long) {
        val passCheckBinding = DialogPasscheckBinding.inflate(mLayoutInflater)
        val dialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_passcheck, passCheckBinding.root)
            .apply {
                passCheckBinding.passcheckStar.setImageResource(R.drawable.star_empty)
                passCheckBinding.passcheckNum.text = checkNum

                mViewModel.getPassTimes(mGetUsername(), checkNum.toInt()) { times ->
                    passCheckBinding.passcheckPasstimes.text = times
                }

                passCheckBinding.passcheckClose.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    hidePassDialog(this)
                }

                passCheckBinding.passcheckStart.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    hidePassDialog(this)
                    mOpenPlayPage(mCreatePlayIntent(checkNum), false)
                }
            }

        showPassDialog(dialog, delayMillis)
    }

    private fun showPassDialog(dialog: MyDialog, delayMillis: Long) {
        mPendingShowDialog?.let(mHandler::removeCallbacks)
        mActiveDialog = dialog
        val showRunnable = Runnable {
            setMapInteractionEnabled(true)
            MyDialogManager.getInstance().show(dialog)
        }
        mPendingShowDialog = showRunnable
        mHandler.postDelayed(showRunnable, delayMillis)
    }

    private fun dismissActivePassDialog() {
        mPendingShowDialog?.let(mHandler::removeCallbacks)
        mPendingShowDialog = null
        mActiveDialog?.let { dialog ->
            hidePassDialog(dialog)
        }
        mActiveDialog = null
    }

    private fun hidePassDialog(dialog: MyDialog) {
        MyDialogManager.getInstance().hide(dialog)
        if (mActiveDialog === dialog) mActiveDialog = null
    }

    private fun setMapInteractionEnabled(enabled: Boolean) {
        mInteractionLocked = !enabled
        mSetMapInteractionEnabled(enabled)
    }

    private companion object {
        private const val MAP_COMPLETION_SCROLL_DELAY_MILLIS = 700L
        private const val WIN_NEXT_DIALOG_DELAY_MILLIS = 1550L
        private const val LOSE_DIALOG_DELAY_MILLIS = 500L
    }
}
