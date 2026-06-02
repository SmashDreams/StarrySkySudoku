package com.bird.starryskysudoku.media

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R

class BgmMusicService : Service() {
    private var mPlayer: MediaPlayer? = null

    inner class LocalBinder : Binder() {
        fun service(): BgmMusicService = this@BgmMusicService
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    fun playIfEnabled() {
        if (!isMusicEnabled()) return
        play()
    }

    fun pause() {
        try {
            if (mPlayer?.isPlaying == true) mPlayer?.pause()
        } catch (exception: IllegalStateException) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        try {
            mPlayer?.stop()
        } catch (exception: IllegalStateException) {
            /* Player may already be stopped; release below still clears the service state. */
        }
        mPlayer?.release()
        mPlayer = null
    }

    private fun play() {
        val player = mPlayer ?: MediaPlayer.create(this, R.raw.bgm).apply {
            isLooping = true
            setVolume(BGM_VOLUME, BGM_VOLUME)
            mPlayer = this
        }
        try {
            if (!player.isPlaying) player.start()
        } catch (exception: IllegalStateException) {
            stopPlayback()
        }
    }

    private fun isMusicEnabled(): Boolean {
        return getSharedPreferences(AppSettings.PREFS_MUSIC, MODE_PRIVATE)
            .getBoolean(AppSettings.KEY_MUSIC, true)
    }

    private companion object {
        private const val BGM_VOLUME = 0.2f
    }
}
