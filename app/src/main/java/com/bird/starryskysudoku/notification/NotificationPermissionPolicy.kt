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
        hasWarmupAttempted: Boolean
    ): Boolean {
        // 某些华为机型在旧系统上需要先走一轮通知预热，避免授权页打断跳转。
        return sdkInt < Build.VERSION_CODES.TIRAMISU &&
            manufacturer.equals(HUAWEI_MANUFACTURER, ignoreCase = true) &&
            !hasWarmupAttempted
    }
}
