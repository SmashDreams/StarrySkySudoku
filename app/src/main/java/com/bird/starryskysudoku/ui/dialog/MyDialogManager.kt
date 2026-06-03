package com.bird.starryskysudoku.ui.dialog

import android.content.Context
import android.view.Gravity
import android.view.View
import com.bird.starryskysudoku.R

class MyDialogManager private constructor() {

    companion object {
        @Volatile
        private var sInstance: MyDialogManager? = null

        fun getInstance(): MyDialogManager {
            return sInstance ?: synchronized(this) {
                sInstance ?: MyDialogManager().also { sInstance = it }
            }
        }
    }

    fun initView(context: Context, layout: Int): MyDialog {
        return MyDialog(context, layout, R.style.MyDialog, Gravity.CENTER)
    }

    fun initView(context: Context, layout: Int, contentView: View): MyDialog {
        return MyDialog(context, layout, contentView, R.style.MyDialog, Gravity.CENTER)
    }

    fun show(dialog: MyDialog) {
        // 统一做显隐保护，避免重复显示或关闭导致状态异常。
        if (!dialog.isShowing) dialog.show()
    }

    fun hide(dialog: MyDialog) {
        if (dialog.isShowing) dialog.dismiss()
    }

    fun hideImmediately(dialog: MyDialog) {
        if (dialog.isShowing) dialog.dismissImmediately()
    }
}
