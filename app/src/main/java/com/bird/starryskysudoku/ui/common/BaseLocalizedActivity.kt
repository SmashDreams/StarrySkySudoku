package com.bird.starryskysudoku.ui.common

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

open class BaseLocalizedActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        // 所有页面在创建前先套用当前语言上下文，避免首帧先显示系统语言再切换。
        super.attachBaseContext(AppLocaleContext.wrap(newBase))
    }
}
