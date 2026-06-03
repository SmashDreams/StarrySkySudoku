package com.bird.starryskysudoku

import android.app.Application
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.AppForegroundBgmController
import com.bird.starryskysudoku.media.PlayMusic

class StarrySkySudokuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 全局音效播放器在应用启动时完成初始化，后续页面直接复用同一实例。
        PlayMusic.getInstance().init(this)
        // 由应用级生命周期统一接管背景音乐，避免每个页面重复维护播放状态。
        registerActivityLifecycleCallbacks(AppForegroundBgmController(this))
        DatabaseInitializer.getDatabase(this)
    }
}
