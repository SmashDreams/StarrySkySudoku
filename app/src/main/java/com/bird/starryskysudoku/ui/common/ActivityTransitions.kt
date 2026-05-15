package com.bird.starryskysudoku.ui.common

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import androidx.annotation.AnimRes

fun Activity.startActivityWithTransition(
    intent: Intent,
    @AnimRes enterAnim: Int,
    @AnimRes exitAnim: Int
) {
    val options = ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim)
    startActivity(intent, options.toBundle())
}

fun Activity.finishWithTransition(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
    }
    finish()
}
