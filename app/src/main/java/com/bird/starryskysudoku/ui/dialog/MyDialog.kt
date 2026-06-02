package com.bird.starryskysudoku.ui.dialog

import android.content.Context
import android.util.Log
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic

class MyDialog : ComponentDialog {

    companion object {
        private const val TAG = "MyDialog"
    }

    private val layout: Int

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
                // 只有设置和关卡确认弹窗允许返回键直接关闭，其余弹窗维持原交互约束。
                if (layout == R.layout.dialog_passcheck || layout == R.layout.dialog_settings) {
                    dismiss()
                    PlayMusic.getInstance().stopDialogShow()
                }
            }
        })
    }

    override fun cancel() {
        Log.d(TAG, "cancel:")
        super.cancel()
        // 暂停弹窗关闭后仍要保留暂停状态，这里不主动停止弹窗音效。
        if (layout != R.layout.dialog_pause) {
            PlayMusic.getInstance().stopDialogShow()
        }
    }
}
