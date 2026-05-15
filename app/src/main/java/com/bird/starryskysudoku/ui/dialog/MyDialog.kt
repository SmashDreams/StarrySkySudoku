package com.bird.starryskysudoku.ui.dialog

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic

class MyDialog(context: Context, private val layout: Int, style: Int, gravity: Int) : Dialog(context, style) {

    companion object {
        private const val TAG = "MyDialog"
    }

    init {
        setContentView(layout)
        window?.attributes?.windowAnimations = R.style.SlideInFromBottomDialogAnimation
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyDown:")
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (layout == R.layout.dialog_passcheck || layout == R.layout.dialog_settings) {
                dismiss()
                PlayMusic.getInstance().stopDialogShow()
                return super.onKeyDown(keyCode, event)
            } else {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun cancel() {
        Log.d(TAG, "cancel:")
        super.cancel()
        if (layout != R.layout.dialog_pause) {
            PlayMusic.getInstance().stopDialogShow()
        }
    }
}
