package com.bird.starryskysudoku.notification

import android.content.pm.PackageManager
import android.os.Build

object NotificationPermissionPolicy {
    private const val HUAWEI_MANUFACTURER = "HUAWEI"

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
        return sdkInt < Build.VERSION_CODES.TIRAMISU &&
            manufacturer.equals(HUAWEI_MANUFACTURER, ignoreCase = true) &&
            !hasWarmupAttempted
    }
}
