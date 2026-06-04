package com.bird.starryskysudoku.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.ui.play.PlayRoute

class CountdownTimerService : Service() {

    private val mHandler = Handler(Looper.getMainLooper())
    private var mLevelNumber = CountdownTimerContract.MIN_LEVEL_NUMBER
    private var mUsername = LauncherSessionReader.GUEST_USERNAME
    private var mEndTimeMs = 0L
    private var mPausedRemaining = 0
    private var mIsPaused = false

    private val mTickRunnable = object : Runnable {
        override fun run() {
            if (mIsPaused) return
            val remaining = computeRemainingSeconds()
            if (remaining <= 0) {
                sendCountdownBroadcast(0)
                updateNotification(0)
                stopSelf()
                return
            }
            sendCountdownBroadcast(remaining)
            updateNotification(remaining)
            val nextTickMs = mEndTimeMs - (remaining - 1L) * 1000L
            val delay = nextTickMs - SystemClock.elapsedRealtime()
            mHandler.postDelayed(this, delay.coerceIn(500L, 1050L))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            CountdownTimerContract.ACTION_PAUSE_TIMER -> {
                pauseTimer()
                return START_NOT_STICKY
            }
            CountdownTimerContract.ACTION_RESUME_TIMER -> {
                resumeTimer()
                return START_NOT_STICKY
            }
            else -> {
                return handleStart(intent)
            }
        }
    }

    private fun handleStart(intent: Intent?): Int {
        val initialSeconds = intent
            ?.getIntExtra(CountdownTimerContract.EXTRA_INITIAL_SECONDS, CountdownTimerContract.DEFAULT_TOTAL_SECONDS)
            ?.let(CountdownTimerContract::normalizeInitialSeconds)
            ?: CountdownTimerContract.DEFAULT_TOTAL_SECONDS
        mLevelNumber = intent
            ?.getIntExtra(CountdownTimerContract.EXTRA_LEVEL_NUMBER, CountdownTimerContract.MIN_LEVEL_NUMBER)
            ?.let(CountdownTimerContract::normalizeLevelNumber)
            ?: CountdownTimerContract.MIN_LEVEL_NUMBER
        mUsername = intent
            ?.getStringExtra(CountdownTimerContract.EXTRA_USERNAME)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: LauncherSessionReader.GUEST_USERNAME
        mIsPaused = false
        startForegroundCountdown(initialSeconds)
        startCountdown(initialSeconds)
        return START_NOT_STICKY
    }

    private fun pauseTimer() {
        if (mIsPaused) return
        mPausedRemaining = computeRemainingSeconds()
        mIsPaused = true
        mHandler.removeCallbacks(mTickRunnable)
        // 广播当前剩余时间，让游戏 UI 和通知显示同步
        sendCountdownBroadcast(mPausedRemaining)
        updateNotification(mPausedRemaining)
    }

    private fun resumeTimer() {
        if (!mIsPaused) return
        mIsPaused = false
        mEndTimeMs = SystemClock.elapsedRealtime() + mPausedRemaining * 1000L
        // 恢复时也广播一次，让 UI 立刻拿到最新时间
        sendCountdownBroadcast(mPausedRemaining)
        mHandler.post(mTickRunnable)
    }

    override fun onDestroy() {
        mHandler.removeCallbacks(mTickRunnable)
        super.onDestroy()
    }

    private fun startForegroundCountdown(initialSeconds: Int) {
        ensureNotificationChannel()
        startForeground(
            CountdownTimerContract.NOTIFICATION_ID,
            buildNotification(initialSeconds)
        )
    }

    private fun startCountdown(initialSeconds: Int) {
        mHandler.removeCallbacks(mTickRunnable)
        if (initialSeconds <= 0) {
            sendCountdownBroadcast(0)
            updateNotification(0)
            stopSelf()
            return
        }
        mEndTimeMs = SystemClock.elapsedRealtime() + initialSeconds * 1000L
        mHandler.post(mTickRunnable)
    }

    private fun computeRemainingSeconds(): Int {
        if (mIsPaused) return mPausedRemaining
        val remaining = (mEndTimeMs - SystemClock.elapsedRealtime() + 500L) / 1000L
        return remaining.coerceAtLeast(0).toInt()
    }

    private fun sendCountdownBroadcast(remainingSeconds: Int) {
        val tickIntent = Intent(CountdownTimerContract.ACTION_COUNTDOWN_TICK).apply {
            setPackage(packageName)
            putExtra(CountdownTimerContract.EXTRA_REMAINING_SECONDS, remainingSeconds)
        }
        sendBroadcast(tickIntent)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CountdownTimerContract.NOTIFICATION_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CountdownTimerContract.NOTIFICATION_CHANNEL_ID,
            getString(R.string.countdown_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            description = getString(R.string.countdown_notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(remainingSeconds: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            PlayRoute.create(this, mLevelNumber, username = mUsername).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val displaySeconds = if (mIsPaused) mPausedRemaining else remainingSeconds
        val contentText = if (mIsPaused) {
            getString(R.string.countdown_notification_remaining, CountdownTimerContract.formatRemainingTime(displaySeconds)) + " · 暂停"
        } else {
            getString(R.string.countdown_notification_remaining, CountdownTimerContract.formatRemainingTime(displaySeconds))
        }
        return NotificationCompat.Builder(this, CountdownTimerContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(CountdownTimerContract.formatLevelTitle(mLevelNumber))
            .setContentText(contentText)
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
            /* 用户可能撤销了通知权限 */
        }
    }
}
