package com.bird.starryskysudoku.ui.play

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bird.starryskysudoku.timer.CountdownTimerContract
import com.bird.starryskysudoku.timer.CountdownTimerService

class CountdownCoordinator(
    private val mActivity: AppCompatActivity,
    private val mGetRemainingSeconds: () -> Int,
    private val mGetLevel: () -> Int,
    private val mCanStart: () -> Boolean,
    private val mOnTick: (Int) -> Unit
) {
    private var mReceiverRegistered = false
    private val mCountdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != CountdownTimerContract.ACTION_COUNTDOWN_TICK) return
            // 页面不自己持有计时器，只接收服务广播回来的剩余秒数。
            val remainingSeconds = intent.getIntExtra(
                CountdownTimerContract.EXTRA_REMAINING_SECONDS,
                CountdownTimerContract.DEFAULT_TOTAL_SECONDS
            )
            mOnTick(remainingSeconds)
        }
    }

    fun onStart() {
        if (mReceiverRegistered) return
        // 只在页面可见时注册接收器，避免后台页面继续消费倒计时广播。
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
        // 每次重启服务时都带上当前剩余时间，让暂停恢复不会丢进度。
        val serviceIntent = Intent(mActivity, CountdownTimerService::class.java)
            .putExtra(CountdownTimerContract.EXTRA_INITIAL_SECONDS, mGetRemainingSeconds())
            .putExtra(CountdownTimerContract.EXTRA_LEVEL_NUMBER, mGetLevel())
        try {
            mActivity.startService(serviceIntent)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Unable to start countdown service while app state is changing", exception)
        }
    }

    fun stop() {
        mActivity.stopService(Intent(mActivity, CountdownTimerService::class.java))
    }

    private companion object {
        private const val TAG = "CountdownCoordinator"
    }
}
