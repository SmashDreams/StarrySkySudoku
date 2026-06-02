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
    // 统一封装页面切换动画，减少各页面重复拼装转场参数。
    val options = ActivityOptions.makeCustomAnimation(this, enterAnim, exitAnim)
    startActivity(intent, options.toBundle())
}

fun Activity.finishWithTransition(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) {
    // 新系统关闭页动画需要单独调用覆盖接口，旧系统保持默认结束行为。
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
    }
    finish()
}
