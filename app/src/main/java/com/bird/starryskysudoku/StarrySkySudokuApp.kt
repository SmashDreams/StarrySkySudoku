package com.bird.starryskysudoku

import android.app.Application
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.PlayMusic

class StarrySkySudokuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PlayMusic.getInstance().init(this)
        DatabaseInitializer.getDatabase(this)
    }
}
