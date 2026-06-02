package com.bird.starryskysudoku.media

import android.content.Context
import com.bird.starryskysudoku.AppSettings

object BgmMusicController {
    private var mService: BgmMusicService? = null

    fun onServiceBound(service: BgmMusicService) {
        mService = service
        playIfEnabled(service)
    }

    fun onServiceUnbound(service: BgmMusicService?) {
        if (mService === service || service == null) mService = null
    }

    fun playIfEnabled(context: Context) {
        if (!isMusicEnabled(context)) return
        mService?.playIfEnabled()
    }

    fun pause() {
        mService?.pause()
    }

    fun stop(context: Context) {
        mService?.stopPlayback()
    }

    private fun isMusicEnabled(context: Context): Boolean {
        return context.applicationContext
            .getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
            .getBoolean(AppSettings.KEY_MUSIC, true)
    }
}
