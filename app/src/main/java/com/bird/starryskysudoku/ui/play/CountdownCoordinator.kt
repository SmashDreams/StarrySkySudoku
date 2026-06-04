package com.bird.starryskysudoku.ui.play

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bird.starryskysudoku.timer.CountdownTimerContract
import com.bird.starryskysudoku.timer.CountdownTimerService

class CountdownCoordinator(
    private val mActivity: AppCompatActivity,
    private val mGetRemainingSeconds: () -> Int,
    private val mGetLevel: () -> Int,
    private val mGetUsername: () -> String,
    private val mCanStart: () -> Boolean,
    private val mOnTick: (Int) -> Unit
) {
    private var mReceiverRegistered = false
    private var mServiceStarted = false
    private val mCountdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != CountdownTimerContract.ACTION_COUNTDOWN_TICK) return
            val remainingSeconds = intent.getIntExtra(
                CountdownTimerContract.EXTRA_REMAINING_SECONDS,
                CountdownTimerContract.DEFAULT_TOTAL_SECONDS
            )
            mOnTick(remainingSeconds)
        }
    }

    fun onStart() {
        if (mReceiverRegistered) return
        ContextCompat.registerReceiver(
            mActivity,
            mCountdownReceiver,
            IntentFilter(CountdownTimerContract.ACTION_COUNTDOWN_TICK),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        mReceiverRegistered = true
    }

    fun onStop() {
        if (!mReceiverRegistered) return
        mActivity.unregisterReceiver(mCountdownReceiver)
        mReceiverRegistered = false
    }

    fun start() {
        if (!mCanStart()) return
        mServiceStarted = true
        val serviceIntent = Intent(mActivity, CountdownTimerService::class.java)
            .putExtra(CountdownTimerContract.EXTRA_INITIAL_SECONDS, mGetRemainingSeconds())
            .putExtra(CountdownTimerContract.EXTRA_LEVEL_NUMBER, mGetLevel())
            .putExtra(CountdownTimerContract.EXTRA_USERNAME, mGetUsername())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mActivity.startForegroundService(serviceIntent)
            } else {
                mActivity.startService(serviceIntent)
            }
        } catch (exception: RuntimeException) {
            Log.w(TAG, "app 状态切换期间无法启动倒计时 service", exception)
        }
    }

    fun stop() {
        if (!mServiceStarted) return
        mServiceStarted = false
        mActivity.stopService(Intent(mActivity, CountdownTimerService::class.java))
    }

    fun pause() {
        // 暂停计时但保留前台服务和通知显示
        if (!mServiceStarted) return
        val intent = Intent(mActivity, CountdownTimerService::class.java).apply {
            action = CountdownTimerContract.ACTION_PAUSE_TIMER
        }
        try {
            mActivity.startService(intent)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "暂停倒计时 service 失败", exception)
        }
    }

    fun resume() {
        // 从暂停状态恢复计时
        if (!mServiceStarted) return
        val intent = Intent(mActivity, CountdownTimerService::class.java).apply {
            action = CountdownTimerContract.ACTION_RESUME_TIMER
        }
        try {
            mActivity.startService(intent)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "恢复倒计时 service 失败", exception)
        }
    }

    private companion object {
        private const val TAG = "CountdownCoordinator"
    }
}
