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
    private val mGetUsername: () -> String,
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
        // 页面恢复或重新进入时把关卡、用户名和剩余秒数一并交给服务，保证倒计时可无缝续跑。
        val serviceIntent = Intent(mActivity, CountdownTimerService::class.java)
            .putExtra(CountdownTimerContract.EXTRA_INITIAL_SECONDS, mGetRemainingSeconds())
            .putExtra(CountdownTimerContract.EXTRA_LEVEL_NUMBER, mGetLevel())
            .putExtra(CountdownTimerContract.EXTRA_USERNAME, mGetUsername())
        try {
            mActivity.startService(serviceIntent)
        } catch (exception: RuntimeException) {
            // 页面正在切换前后台或销毁时，启动服务可能短暂失败，这里只做保护性兜底。
            Log.w(TAG, "app 状态切换期间无法启动倒计时 service", exception)
        }
    }

    fun stop() {
        mActivity.stopService(Intent(mActivity, CountdownTimerService::class.java))
    }

    private companion object {
        private const val TAG = "CountdownCoordinator"
    }
}
