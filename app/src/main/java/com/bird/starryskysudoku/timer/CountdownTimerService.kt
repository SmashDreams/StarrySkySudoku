package com.bird.starryskysudoku.timer

import android.app.Service
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder

class CountdownTimerService : Service() {

    /*
     * 后台服务只负责维护倒计时，不直接接触界面控件。
     * 页面通过动态注册的广播接收器接收剩余时间，避免页面和计时逻辑强耦合。
     */
    private var mTimer: CountDownTimer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        /*
         * 每次启动服务都使用最新剩余时间重新计时；
         * 参数会被限制在一局游戏的有效范围内，避免异常调用拖长服务生命周期。
         */
        val initialSeconds = intent
            ?.getIntExtra(CountdownTimerContract.EXTRA_INITIAL_SECONDS, CountdownTimerContract.DEFAULT_TOTAL_SECONDS)
            ?.let(CountdownTimerContract::normalizeInitialSeconds)
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
         * 后台服务负责游戏倒计时，页面不直接持有计时器；
         * 每秒通过系统广播把剩余时间发送给前台动态接收器。
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
            /*
             * 广播只投递给本应用包名，避免把实时游戏状态泄露给其他应用。
             */
            setPackage(packageName)
            putExtra(CountdownTimerContract.EXTRA_REMAINING_SECONDS, remainingSeconds)
        }
        sendBroadcast(tickIntent)
    }
}
