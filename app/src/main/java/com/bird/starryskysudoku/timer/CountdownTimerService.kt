package com.bird.starryskysudoku.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.ui.play.PlayActivity

class CountdownTimerService : Service() {

    /*
     * 后台服务只负责维护倒计时，不直接接触界面控件。
     * 页面通过动态注册的广播接收器接收剩余时间，避免页面和计时逻辑强耦合。
     */
    private var mTimer: CountDownTimer? = null
    private var mLevelNumber = CountdownTimerContract.MIN_LEVEL_NUMBER

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
        mLevelNumber = intent
            ?.getIntExtra(CountdownTimerContract.EXTRA_LEVEL_NUMBER, CountdownTimerContract.MIN_LEVEL_NUMBER)
            ?.let(CountdownTimerContract::normalizeLevelNumber)
            ?: CountdownTimerContract.MIN_LEVEL_NUMBER
        startForegroundCountdown(initialSeconds)
        startCountdown(initialSeconds)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mTimer?.cancel()
        mTimer = null
        super.onDestroy()
    }

    private fun startForegroundCountdown(initialSeconds: Int) {
        createNotificationChannel()
        startForeground(
            CountdownTimerContract.NOTIFICATION_ID,
            buildNotification(initialSeconds)
        )
    }

    private fun startCountdown(initialSeconds: Int) {
        mTimer?.cancel()
        if (initialSeconds <= 0) {
            sendCountdownBroadcast(0)
            updateNotification(0)
            stopSelf()
            return
        }

        /*
         * 后台服务负责游戏倒计时，页面不直接持有计时器；
         * 每秒通过系统广播把剩余时间发送给前台动态接收器。
         */
        mTimer = object : CountDownTimer(initialSeconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSeconds = (millisUntilFinished / 1000L).toInt()
                sendCountdownBroadcast(remainingSeconds)
                updateNotification(remainingSeconds)
            }

            override fun onFinish() {
                sendCountdownBroadcast(0)
                updateNotification(0)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CountdownTimerContract.NOTIFICATION_CHANNEL_ID,
            getString(R.string.countdown_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            description = getString(R.string.countdown_notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(remainingSeconds: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, PlayActivity::class.java).apply {
                putExtra("mNum", mLevelNumber.toString())
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CountdownTimerContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(CountdownTimerContract.formatLevelTitle(mLevelNumber))
            .setContentText(getString(R.string.countdown_notification_remaining, CountdownTimerContract.formatRemainingTime(remainingSeconds)))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(remainingSeconds: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(this).notify(
                CountdownTimerContract.NOTIFICATION_ID,
                buildNotification(remainingSeconds)
            )
        } catch (exception: SecurityException) {
            /*
             * Android 13+ 允许用户撤销通知权限；倒计时广播仍继续驱动游戏界面。
             */
        }
    }
}
