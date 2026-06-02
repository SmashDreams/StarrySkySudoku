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
            Log.w(TAG, "Unable to bind BGM service while app state is changing", exception)
            mBound = false
        }
    }

    private fun unbind() {
        if (!mBound) return
        BgmMusicController.onServiceUnbound(mBoundService)
        try {
            mApplication.unbindService(mConnection)
        } catch (exception: IllegalArgumentException) {
            Log.w(TAG, "BGM service was already unbound", exception)
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
        private const val BACKGROUND_UNBIND_DELAY_MS = 100L
    }
}
