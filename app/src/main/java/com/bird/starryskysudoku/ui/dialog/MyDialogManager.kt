package com.bird.starryskysudoku.ui.dialog

import android.content.Context
import android.view.Gravity
import com.bird.starryskysudoku.R

class MyDialogManager private constructor() {

    companion object {
        @Volatile
        private var instance: MyDialogManager? = null

        fun getInstance(): MyDialogManager {
            return instance ?: synchronized(this) {
                instance ?: MyDialogManager().also { instance = it }
            }
        }
    }

    fun initView(context: Context, layout: Int): MyDialog {
        return MyDialog(context, layout, R.style.MyDialog, Gravity.CENTER)
    }

    fun show(dialog: MyDialog) {
        if (!dialog.isShowing) dialog.show()
    }

    fun hide(dialog: MyDialog) {
        if (dialog.isShowing) dialog.dismiss()
    }
}
