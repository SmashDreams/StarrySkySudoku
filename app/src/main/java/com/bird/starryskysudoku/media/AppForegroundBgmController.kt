package com.bird.starryskysudoku.media

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class AppForegroundBgmController(
    private val mApplication: Application
) : Application.ActivityLifecycleCallbacks {
    private val mHandler = Handler(Looper.getMainLooper())
    private var mStartedActivities = 0
    /*
     * 页面切换时会短暂经历旧页面停止和新页面启动，
     * 这里延迟暂停，避免前后台判断过于敏感导致背景音乐被切断。
     */
    private val mPauseRunnable = Runnable { BgmMusicController.pause(mApplication) }

    override fun onActivityStarted(activity: Activity) {
        mStartedActivities++
        mHandler.removeCallbacks(mPauseRunnable)
        if (mStartedActivities == 1) {
            // 应用从后台回到前台时恢复背景音乐。
            BgmMusicController.playIfEnabled(mApplication)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        mStartedActivities = (mStartedActivities - 1).coerceAtLeast(0)
        if (mStartedActivities == 0) {
            mHandler.postDelayed(mPauseRunnable, BACKGROUND_PAUSE_DELAY_MS)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) {
        mHandler.removeCallbacks(mPauseRunnable)
        // 某些机型会先进入恢复态再稳定进入前台，这里再兜底一次恢复播放。
        BgmMusicController.playIfEnabled(mApplication)
    }
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        // 给页面切换预留一个极短缓冲时间，避免误判为退到后台。
        private const val BACKGROUND_PAUSE_DELAY_MS = 100L
    }
}
