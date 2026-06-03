package com.bird.starryskysudoku.media

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class AppForegroundBgmController(
    private val mApplication: Application
) : Application.ActivityLifecycleCallbacks {
    // 应用前后台切换时由这里统一绑定、续播和延迟解绑背景音乐服务。
    private val mHandler = Handler(Looper.getMainLooper())
    private var mStartedActivities = 0
    private var mBound = false
    private var mBoundService: BgmMusicService? = null
    private val mUnbindRunnable = Runnable { unbind() }
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as? BgmMusicService.LocalBinder)?.service() ?: return
            mBoundService = service
            BgmMusicController.onServiceBound(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            BgmMusicController.onServiceUnbound(mBoundService)
            mBoundService = null
            mBound = false
        }
    }

    override fun onActivityStarted(activity: Activity) {
        mStartedActivities++
        mHandler.removeCallbacks(mUnbindRunnable)
        // 只要应用重新回到前台可见状态，就立即确保音乐服务已重新绑定。
        bind(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        mHandler.removeCallbacks(mUnbindRunnable)
        bind(activity)
        BgmMusicController.playIfEnabled(activity)
    }

    override fun onActivityStopped(activity: Activity) {
        mStartedActivities = (mStartedActivities - 1).coerceAtLeast(0)
        if (mStartedActivities == 0) {
            // 最后一个页面离开前台时先暂停音乐，再延迟解绑，兼容快速页面切换抖动。
            BgmMusicController.pause()
            mHandler.postDelayed(mUnbindRunnable, BACKGROUND_UNBIND_DELAY_MS)
        }
    }

    private fun bind(activity: Activity) {
        if (mBound) return
        try {
            mBound = mApplication.bindService(
                Intent(mApplication, BgmMusicService::class.java),
                mConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (exception: RuntimeException) {
            // 应用切后台或进程状态切换瞬间可能绑定失败，这里只做保护性兜底。
            Log.w(TAG, "app 状态切换期间无法绑定背景音乐 service", exception)
            mBound = false
        }
    }

    private fun unbind() {
        if (!mBound) return
        // 解绑前先通知控制器丢弃旧服务引用，避免后续误操作已经失效的 Binder。
        BgmMusicController.onServiceUnbound(mBoundService)
        try {
            mApplication.unbindService(mConnection)
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "背景音乐 service 已经解绑", exception)
        }
        mBoundService = null
        mBound = false
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        private const val TAG = "AppForegroundBgm"
        // 留一个极短延迟，避免页面切换过程中频繁解绑又立刻重绑。
        private const val BACKGROUND_UNBIND_DELAY_MS = 100L
    }
}
