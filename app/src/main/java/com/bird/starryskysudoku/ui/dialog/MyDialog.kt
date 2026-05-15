package com.bird.starryskysudoku.ui.dialog

import android.content.Context
import android.util.Log
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic

class MyDialog(context: Context, private val layout: Int, style: Int, gravity: Int) : ComponentDialog(context, style) {

    companion object {
        private const val TAG = "MyDialog"
    }

    init {
        setContentView(layout)
        window?.setGravity(gravity)
        window?.attributes?.windowAnimations = R.style.SlideInFromBottomDialogAnimation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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
        if (layout != R.layout.dialog_pause) {
            PlayMusic.getInstance().stopDialogShow()
        }
    }
}
