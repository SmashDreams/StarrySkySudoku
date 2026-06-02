package com.bird.starryskysudoku.media

import android.content.Context
import android.content.Intent
import com.bird.starryskysudoku.AppSettings

object BgmMusicController {
    /*
     * 页面侧不直接操作背景音乐服务，
     * 统一经由这里转发播放、暂停和停止命令。
     */
    fun playIfEnabled(context: Context) {
        if (!isMusicEnabled(context)) return
        send(context, BgmMusicService.ACTION_PLAY)
    }

    fun pause(context: Context) {
        /*
         * 仅暂停当前运行实例，保留播放器位置，
         * 方便应用重新回到前台后继续播放。
         */
        BgmMusicService.pauseRunningInstance()
    }

    fun stop(context: Context) {
        send(context, BgmMusicService.ACTION_STOP)
    }

    private fun send(context: Context, action: String) {
        // 一律使用应用上下文发送命令，避免把页面实例意外绑定进服务生命周期。
        context.applicationContext.startService(
            Intent(context.applicationContext, BgmMusicService::class.java).setAction(action)
        )
    }

    private fun isMusicEnabled(context: Context): Boolean {
        // 背景音乐与设置页共用同一组偏好配置。
        return context.applicationContext
            .getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
            .getBoolean(AppSettings.KEY_MUSIC, true)
    }
}
