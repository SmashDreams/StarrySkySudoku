package com.bird.starryskysudoku

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.PlayMusic

class StarrySkySudokuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val language = getSharedPreferences("language", Context.MODE_PRIVATE)
            .getString("language", "zh") ?: "zh"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        PlayMusic.getInstance().init(this)
        DatabaseInitializer.getDatabase(this)
    }
}
