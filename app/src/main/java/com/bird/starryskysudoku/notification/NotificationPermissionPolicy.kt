package com.bird.starryskysudoku.notification

import android.content.pm.PackageManager
import android.os.Build

object NotificationPermissionPolicy {
    private const val HUAWEI_MANUFACTURER = "HUAWEI"

    // 十三及以上系统才需要显式通知权限。
    fun shouldRequestPostNotifications(sdkInt: Int, permissionStatus: Int): Boolean {
        return sdkInt >= Build.VERSION_CODES.TIRAMISU &&
            permissionStatus != PackageManager.PERMISSION_GRANTED
    }

    fun shouldStartPlayImmediately(sdkInt: Int, permissionStatus: Int): Boolean {
        return !shouldRequestPostNotifications(sdkInt, permissionStatus)
    }

    fun shouldWarmUpVendorNotificationsBeforePlay(
        sdkInt: Int,
        manufacturer: String,
        hasProcessWarmupCompleted: Boolean
    ): Boolean {
        // 不持久化预热结果；同一进程内只预热一次，避免每次进入棋盘都产生明显延迟。
        return sdkInt < Build.VERSION_CODES.TIRAMISU &&
            manufacturer.equals(HUAWEI_MANUFACTURER, ignoreCase = true) &&
            !hasProcessWarmupCompleted
    }
}
