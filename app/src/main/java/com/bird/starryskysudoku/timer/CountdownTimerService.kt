package com.bird.starryskysudoku.timer

import android.app.Service
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder

class CountdownTimerService : Service() {

    private var mTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val initialSeconds = intent
            ?.getIntExtra(CountdownTimerContract.EXTRA_INITIAL_SECONDS, CountdownTimerContract.DEFAULT_TOTAL_SECONDS)
            ?.coerceAtLeast(0)
            ?: CountdownTimerContract.DEFAULT_TOTAL_SECONDS
        startCountdown(initialSeconds)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mTimer?.cancel()
        mTimer = null
        super.onDestroy()
    }

    private fun startCountdown(initialSeconds: Int) {
        mTimer?.cancel()
        if (initialSeconds <= 0) {
            sendCountdownBroadcast(0)
            stopSelf()
            return
        }

        /*
         * Service 负责游戏倒计时，Activity 不直接持有计时器；
         * 每秒通过系统 Broadcast 把剩余时间发送给前台动态 Receiver。
         */
        mTimer = object : CountDownTimer(initialSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                sendCountdownBroadcast((millisUntilFinished / 1000L).toInt())
            }

            override fun onFinish() {
                sendCountdownBroadcast(0)
                stopSelf()
            }
        }.start()
    }

    private fun sendCountdownBroadcast(remainingSeconds: Int) {
        val tickIntent = Intent(CountdownTimerContract.ACTION_COUNTDOWN_TICK).apply {
            setPackage(packageName)
            putExtra(CountdownTimerContract.EXTRA_REMAINING_SECONDS, remainingSeconds)
        }
        sendBroadcast(tickIntent)
    }
}
