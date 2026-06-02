package com.bird.starryskysudoku

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.AppForegroundBgmController
import com.bird.starryskysudoku.media.PlayMusic

class StarrySkySudokuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val language = getSharedPreferences(AppSettings.PREFS_LANGUAGE, Context.MODE_PRIVATE)
            .getString(AppSettings.KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE) ?: AppSettings.DEFAULT_LANGUAGE
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        PlayMusic.getInstance().init(this)
        // 由应用级生命周期统一接管背景音乐，避免每个页面重复维护播放状态。
        registerActivityLifecycleCallbacks(AppForegroundBgmController(this))
        DatabaseInitializer.getDatabase(this)
    }
}
