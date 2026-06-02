package com.bird.starryskysudoku.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.notification.NotificationPermissionPolicy
import com.bird.starryskysudoku.timer.CountdownTimerContract
import com.bird.starryskysudoku.ui.common.startActivityWithTransition

class MapNotificationNavigator(
    private val mActivity: AppCompatActivity,
    private val mPermissionLauncher: ActivityResultLauncher<String>
) {
    private val mHandler = Handler(Looper.getMainLooper())
    private var mPendingPlayIntent: Intent? = null
    private var mPendingPlayShouldFinishMap = false
    private var mWaitingVendorNotificationWarmup = false
    private var mCanCompleteVendorNotificationWarmup = false
    private var mMapResumed = false
    private var mMapHasWindowFocus = false

    fun openPlayPageAfterNotificationPermission(playIntent: Intent, finishMapAfterStart: Boolean) {
        /*
         * 进入棋盘前先处理通知权限，避免权限弹窗在棋盘页触发暂停逻辑。
         */
        val permissionStatus = ContextCompat.checkSelfPermission(
            mActivity,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (
            NotificationPermissionPolicy.shouldWarmUpVendorNotificationsBeforePlay(
                sdkInt = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER,
                hasProcessWarmupCompleted = sVendorNotificationWarmupCompleted
            )
        ) {
            warmUpVendorNotificationsBeforePlay(playIntent, finishMapAfterStart)
            return
        }

        if (
            NotificationPermissionPolicy.shouldStartPlayImmediately(
                sdkInt = Build.VERSION.SDK_INT,
                permissionStatus = permissionStatus
            )
        ) {
            startPlayPage(playIntent, finishMapAfterStart)
            return
        }

        mPendingPlayIntent = playIntent
        mPendingPlayShouldFinishMap = finishMapAfterStart
        mPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun startPendingPlayPage() {
        val playIntent = mPendingPlayIntent ?: return
        val finishMapAfterStart = mPendingPlayShouldFinishMap
        mPendingPlayIntent = null
        mPendingPlayShouldFinishMap = false
        startPlayPage(playIntent, finishMapAfterStart)
    }

    fun onResume() {
        mMapResumed = true
        completeVendorNotificationWarmupIfReady()
    }

    fun onPause() {
        mMapResumed = false
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        mMapHasWindowFocus = hasFocus
        if (hasFocus) completeVendorNotificationWarmupIfReady()
    }

    fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
        NotificationManagerCompat.from(mActivity).cancel(VENDOR_WARMUP_NOTIFICATION_ID)
    }

    private fun warmUpVendorNotificationsBeforePlay(playIntent: Intent, finishMapAfterStart: Boolean) {
        mPendingPlayIntent = playIntent
        mPendingPlayShouldFinishMap = finishMapAfterStart
        mWaitingVendorNotificationWarmup = true
        mCanCompleteVendorNotificationWarmup = false
        showVendorNotificationPreflight()
        mHandler.postDelayed({
            mCanCompleteVendorNotificationWarmup = true
            completeVendorNotificationWarmupIfReady()
        }, VENDOR_WARMUP_DELAY_MS)
    }

    private fun completeVendorNotificationWarmupIfReady() {
        if (!mWaitingVendorNotificationWarmup) return
        /*
         * 华为 12 版本系统需要等地图页恢复并获得焦点后再进入棋盘，避免授权页覆盖棋盘。
         */
        if (!mCanCompleteVendorNotificationWarmup || !mMapResumed || !mMapHasWindowFocus) return

        NotificationManagerCompat.from(mActivity).cancel(VENDOR_WARMUP_NOTIFICATION_ID)
        sVendorNotificationWarmupCompleted = true
        mWaitingVendorNotificationWarmup = false
        mCanCompleteVendorNotificationWarmup = false
        startPendingPlayPage()
    }

    @SuppressLint("MissingPermission")
    private fun showVendorNotificationPreflight() {
        createNotificationChannel()
        val contentIntent = PendingIntent.getActivity(
            mActivity,
            0,
            Intent(mActivity, MapActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(mActivity, CountdownTimerContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(mActivity.getString(R.string.notification_preflight_title))
            .setContentText(mActivity.getString(R.string.notification_preflight_text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(mActivity).notify(VENDOR_WARMUP_NOTIFICATION_ID, notification)
        } catch (exception: SecurityException) {
            /*
             * 厂商系统可能直接拦截通知；这里不阻塞进入棋盘，只把权限触发尽量提前到地图页。
             */
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CountdownTimerContract.NOTIFICATION_CHANNEL_ID,
            mActivity.getString(R.string.countdown_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            description = mActivity.getString(R.string.countdown_notification_channel_description)
        }
        mActivity.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startPlayPage(playIntent: Intent, finishMapAfterStart: Boolean) {
        mActivity.startActivityWithTransition(
            playIntent,
            R.anim.playpage_show,
            R.anim.playpage_hide
        )
        if (finishMapAfterStart) mActivity.finish()
    }

    private companion object {
        private var sVendorNotificationWarmupCompleted = false
        private const val VENDOR_WARMUP_NOTIFICATION_ID = 1002
        private const val VENDOR_WARMUP_DELAY_MS = 700L
    }
}
