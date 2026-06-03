package com.bird.starryskysudoku.media

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R

class BgmMusicService : Service() {
    // 背景音乐常驻服务只维护长音频播放器，短音效仍由 PlayMusic 单独负责。
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
        // 页面切到后台时只暂停播放，保留播放器实例，方便回前台后快速续播。
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
            /*
             * 播放器可能已经被系统或异常流程提前停止；
             * 这里继续释放资源即可，确保服务状态最终回到干净状态。
             */
        }
        mPlayer?.release()
        mPlayer = null
    }

    private fun play() {
        // 首次播放时创建并缓存播放器，后续直接复用，避免页面切换反复解码音频资源。
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
