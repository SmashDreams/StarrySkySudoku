package com.bird.starryskysudoku.ui.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic

class MyDialog : ComponentDialog {

    companion object {
        private const val TAG = "MyDialog"
        private const val DEFAULT_ENTER_ANIMATION_MILLIS = 800L
    }

    // 弹窗入场动画期间暂时锁住所有可点击控件，避免动画未结束就重复触发操作。
    private val mLayout: Int
    private val mHandler = Handler(Looper.getMainLooper())
    private val mLockedViewStates = LinkedHashMap<View, Boolean>()
    private var mInteractionLockDurationMillis = DEFAULT_ENTER_ANIMATION_MILLIS
    private var mUnlockRunnable: Runnable? = null
    private var mInteractionLocked = false

    constructor(context: Context, layout: Int, style: Int, gravity: Int) : super(context, style) {
        this.mLayout = layout
        setContentView(layout)
        initWindow(gravity)
    }

    constructor(context: Context, layout: Int, contentView: View, style: Int, gravity: Int) : super(context, style) {
        this.mLayout = layout
        setContentView(contentView)
        initWindow(gravity)
    }

    private fun initWindow(gravity: Int) {
        window?.setGravity(gravity)
        window?.attributes?.windowAnimations = R.style.SlideInFromBottomDialogAnimation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mInteractionLocked) return
                // 只有设置和关卡确认弹窗允许返回键直接关闭，其余弹窗维持原交互约束。
                if (mLayout == R.layout.dialog_passcheck || mLayout == R.layout.dialog_settings) {
                    dismiss()
                    PlayMusic.getInstance().stopDialogShow()
                }
            }
        })
    }

    fun setInteractionLockDuration(durationMillis: Long) {
        mInteractionLockDurationMillis = durationMillis.coerceAtLeast(0L)
    }

    override fun show() {
        super.show()
        lockInteractionsUntilEnterAnimationEnds()
    }

    override fun cancel() {
        Log.d(TAG, "弹窗取消")
        if (mInteractionLocked) return
        super.cancel()
        // 暂停弹窗关闭后仍要保留暂停状态，这里不主动停止弹窗音效。
        if (mLayout != R.layout.dialog_pause) {
            PlayMusic.getInstance().stopDialogShow()
        }
    }

    override fun dismiss() {
        mUnlockRunnable?.let(mHandler::removeCallbacks)
        mUnlockRunnable = null
        unlockInteractions()
        super.dismiss()
    }

    fun dismissImmediately() {
        // 某些场景需要立刻移除弹窗，先临时关闭退出动画再恢复原动画配置。
        val windowAttributes = window?.attributes
        val originalAnimation = windowAttributes?.windowAnimations
        if (windowAttributes != null) {
            windowAttributes.windowAnimations = 0
            window?.attributes = windowAttributes
        }
        dismiss()
        if (windowAttributes != null && originalAnimation != null) {
            windowAttributes.windowAnimations = originalAnimation
            window?.attributes = windowAttributes
        }
    }

    private fun lockInteractionsUntilEnterAnimationEnds() {
        mUnlockRunnable?.let(mHandler::removeCallbacks)
        mLockedViewStates.clear()
        mInteractionLocked = true

        // 装饰视图下递归收集所有可交互子控件，统一做短暂禁用。
        val content = window?.decorView ?: run {
            mInteractionLocked = false
            return
        }
        collectClickableViews(content)
        mLockedViewStates.forEach { (view, _) -> view.isEnabled = false }

        val runnable = Runnable { unlockInteractions() }
        mUnlockRunnable = runnable
        mHandler.postDelayed(runnable, mInteractionLockDurationMillis)
    }

    private fun collectClickableViews(view: View) {
        if (view !is ViewGroup && (view.isClickable || view.isLongClickable)) {
            mLockedViewStates[view] = view.isEnabled
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectClickableViews(view.getChildAt(index))
            }
        }
    }

    private fun unlockInteractions() {
        mLockedViewStates.forEach { (view, wasEnabled) -> view.isEnabled = wasEnabled }
        mLockedViewStates.clear()
        mInteractionLocked = false
        mUnlockRunnable = null
    }
}
