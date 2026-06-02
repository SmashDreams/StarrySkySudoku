package com.bird.starryskysudoku.ui.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View

fun View.flashThreeTimes(onEnd: () -> Unit = {}) {
    // 通过六段往返透明度变化实现“三次闪烁”的视觉效果。
    alpha = 1f
    ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0.25f).apply {
        duration = 170L
        repeatCount = 5
        repeatMode = ObjectAnimator.REVERSE
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                alpha = 1f
                onEnd()
            }
        })
        start()
    }
}
