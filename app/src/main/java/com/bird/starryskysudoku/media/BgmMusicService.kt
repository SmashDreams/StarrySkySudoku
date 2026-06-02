package com.bird.starryskysudoku.media

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.bird.starryskysudoku.R

class BgmMusicService : Service() {
    /*
     * 前台页面切换时共用同一个背景音乐播放器，
     * 避免在地图页和棋盘页之间来回切换时重复创建播放器实例。
     */
    private var mPlayer: MediaPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sInstance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopPlayback()
        if (sInstance === this) sInstance = null
        super.onDestroy()
    }

    private fun play() {
        /*
         * 延迟到首次播放时再创建播放器，
         * 这样只有在用户开启音乐时才会真正占用音频资源。
         */
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

    private fun pause() {
        try {
            if (mPlayer?.isPlaying == true) mPlayer?.pause()
        } catch (exception: IllegalStateException) {
            stopPlayback()
        }
    }

    private fun stopPlayback() {
        try {
            mPlayer?.stop()
        } catch (exception: IllegalStateException) {
            /*
             * 播放器可能已经停止；
             * 这里继续向下执行资源释放，保证服务内部状态被彻底清理。
             */
        }
        mPlayer?.release()
        mPlayer = null
    }

    companion object {
        private var sInstance: BgmMusicService? = null

        /*
         * 应用退到后台时直接暂停当前实例，
         * 避免再次发送额外的服务控制命令。
         */
        fun pauseRunningInstance() {
            sInstance?.pause()
        }

        const val ACTION_PLAY = "com.bird.starryskysudoku.media.action.PLAY_BGM"
        const val ACTION_PAUSE = "com.bird.starryskysudoku.media.action.PAUSE_BGM"
        const val ACTION_STOP = "com.bird.starryskysudoku.media.action.STOP_BGM"
        private const val BGM_VOLUME = 0.2f
    }
}
