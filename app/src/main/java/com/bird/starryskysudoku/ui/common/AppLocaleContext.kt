package com.bird.starryskysudoku.ui.common

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.bird.starryskysudoku.AppSettings
import java.util.Locale

object AppLocaleContext {
    // 只有用户明确切换过语言后，才包装上下文覆盖系统语言。
    fun wrap(base: Context): Context {
        val prefs = base.getSharedPreferences(AppSettings.PREFS_LANGUAGE, Context.MODE_PRIVATE)
        if (!prefs.contains(AppSettings.KEY_LANGUAGE)) return base
        val language = prefs.getString(AppSettings.KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE)
            ?: AppSettings.DEFAULT_LANGUAGE
        return base.createConfigurationContext(configurationFor(base, language))
    }

    // 设置页只维护中英文两种选项，这里统一收敛成界面可直接使用的语言标记。
    fun readEffectiveLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(AppSettings.PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val language = if (prefs.contains(AppSettings.KEY_LANGUAGE)) {
            prefs.getString(AppSettings.KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE)
        } else {
            context.resources.configuration.locales[0]?.language ?: Locale.getDefault().language
        }
        return if (language == "en") "en" else "zh"
    }

    @Suppress("DEPRECATION")
    fun applyLanguageToCurrentResources(context: Context, language: String) {
        // 弹窗内切换语言后需要立刻刷新当前界面资源，避免必须重建页面才生效。
        val configuration = configurationFor(context, language)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }

    private fun configurationFor(context: Context, language: String): Configuration {
        // 只按语言标签构造语言环境对象，保证应用内所有资源查找口径一致。
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        return Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
        }
    }
}
