package com.bird.starryskysudoku.notification

import android.content.pm.PackageManager
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {

    @Test
    fun doesNotRequestPermissionBeforeAndroid13() {
        assertFalse(
            NotificationPermissionPolicy.shouldRequestPostNotifications(
                sdkInt = Build.VERSION_CODES.S,
                permissionStatus = PackageManager.PERMISSION_DENIED
            )
        )
    }

    @Test
    fun requestsPermissionOnAndroid13WhenDenied() {
        assertTrue(
            NotificationPermissionPolicy.shouldRequestPostNotifications(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionStatus = PackageManager.PERMISSION_DENIED
            )
        )
    }

    @Test
    fun doesNotRequestPermissionOnAndroid13WhenGranted() {
        assertFalse(
            NotificationPermissionPolicy.shouldRequestPostNotifications(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionStatus = PackageManager.PERMISSION_GRANTED
            )
        )
    }

    @Test
    fun delaysPlayStartOnAndroid13UntilPermissionResultReturns() {
        assertFalse(
            NotificationPermissionPolicy.shouldStartPlayImmediately(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionStatus = PackageManager.PERMISSION_DENIED
            )
        )
    }

    @Test
    fun startsPlayImmediatelyWhenNotificationPermissionIsNotNeeded() {
        assertTrue(
            NotificationPermissionPolicy.shouldStartPlayImmediately(
                sdkInt = Build.VERSION_CODES.S,
                permissionStatus = PackageManager.PERMISSION_DENIED
            )
        )
        assertTrue(
            NotificationPermissionPolicy.shouldStartPlayImmediately(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionStatus = PackageManager.PERMISSION_GRANTED
            )
        )
    }

    @Test
    fun warmsUpHuaweiNotificationsBeforeFirstPlayOnAndroid12() {
        assertTrue(
            NotificationPermissionPolicy.shouldWarmUpVendorNotificationsBeforePlay(
                sdkInt = Build.VERSION_CODES.S,
                manufacturer = "HUAWEI",
                hasWarmupAttempted = false
            )
        )
    }

    @Test
    fun doesNotWarmUpVendorNotificationsAfterAttemptOrOnAndroid13() {
        assertFalse(
            NotificationPermissionPolicy.shouldWarmUpVendorNotificationsBeforePlay(
                sdkInt = Build.VERSION_CODES.S,
                manufacturer = "HUAWEI",
                hasWarmupAttempted = true
            )
        )
        assertFalse(
            NotificationPermissionPolicy.shouldWarmUpVendorNotificationsBeforePlay(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                manufacturer = "HUAWEI",
                hasWarmupAttempted = false
            )
        )
        assertFalse(
            NotificationPermissionPolicy.shouldWarmUpVendorNotificationsBeforePlay(
                sdkInt = Build.VERSION_CODES.S,
                manufacturer = "Google",
                hasWarmupAttempted = false
            )
        )
    }
}
