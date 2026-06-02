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

    private val layout: Int
    private val handler = Handler(Looper.getMainLooper())
    private val lockedViewStates = LinkedHashMap<View, Boolean>()
    private var interactionLockDurationMillis = DEFAULT_ENTER_ANIMATION_MILLIS
    private var unlockRunnable: Runnable? = null
    private var interactionLocked = false

    constructor(context: Context, layout: Int, style: Int, gravity: Int) : super(context, style) {
        this.layout = layout
        setContentView(layout)
        initWindow(gravity)
    }

    constructor(context: Context, layout: Int, contentView: View, style: Int, gravity: Int) : super(context, style) {
        this.layout = layout
        setContentView(contentView)
        initWindow(gravity)
    }

    private fun initWindow(gravity: Int) {
        window?.setGravity(gravity)
        window?.attributes?.windowAnimations = R.style.SlideInFromBottomDialogAnimation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (interactionLocked) return
                // 只有设置和关卡确认弹窗允许返回键直接关闭，其余弹窗维持原交互约束。
                if (layout == R.layout.dialog_passcheck || layout == R.layout.dialog_settings) {
                    dismiss()
                    PlayMusic.getInstance().stopDialogShow()
                }
            }
        })
    }

    fun setInteractionLockDuration(durationMillis: Long) {
        interactionLockDurationMillis = durationMillis.coerceAtLeast(0L)
    }

    override fun show() {
        super.show()
        lockInteractionsUntilEnterAnimationEnds()
    }

    override fun cancel() {
        Log.d(TAG, "cancel:")
        if (interactionLocked) return
        super.cancel()
        // 暂停弹窗关闭后仍要保留暂停状态，这里不主动停止弹窗音效。
        if (layout != R.layout.dialog_pause) {
            PlayMusic.getInstance().stopDialogShow()
        }
    }

    override fun dismiss() {
        unlockRunnable?.let(handler::removeCallbacks)
        unlockRunnable = null
        unlockInteractions()
        super.dismiss()
    }

    private fun lockInteractionsUntilEnterAnimationEnds() {
        unlockRunnable?.let(handler::removeCallbacks)
        lockedViewStates.clear()
        interactionLocked = true

        val content = window?.decorView ?: run {
            interactionLocked = false
            return
        }
        collectClickableViews(content)
        lockedViewStates.forEach { (view, _) -> view.isEnabled = false }

        val runnable = Runnable { unlockInteractions() }
        unlockRunnable = runnable
        handler.postDelayed(runnable, interactionLockDurationMillis)
    }

    private fun collectClickableViews(view: View) {
        if (view !is ViewGroup && (view.isClickable || view.isLongClickable)) {
            lockedViewStates[view] = view.isEnabled
        }
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) {
                collectClickableViews(view.getChildAt(index))
            }
        }
    }

    private fun unlockInteractions() {
        lockedViewStates.forEach { (view, wasEnabled) -> view.isEnabled = wasEnabled }
        lockedViewStates.clear()
        interactionLocked = false
        unlockRunnable = null
    }
}
