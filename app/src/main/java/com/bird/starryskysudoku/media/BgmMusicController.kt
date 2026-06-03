package com.bird.starryskysudoku.media

import android.content.Context
import com.bird.starryskysudoku.AppSettings

object BgmMusicController {
    private var sService: BgmMusicService? = null

    fun onServiceBound(service: BgmMusicService) {
        sService = service
        playIfEnabled(service)
    }

    fun onServiceUnbound(service: BgmMusicService?) {
        if (sService === service || service == null) sService = null
    }

    fun playIfEnabled(context: Context) {
        if (!isMusicEnabled(context)) return
        sService?.playIfEnabled()
    }

    fun pause() {
        sService?.pause()
    }

    fun stop(context: Context) {
        sService?.stopPlayback()
    }

    private fun isMusicEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
            .getBoolean(AppSettings.KEY_MUSIC, true)
    }
}
