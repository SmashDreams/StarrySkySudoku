package com.bird.starryskysudoku.ui.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapNotificationNavigatorStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun vendorNotificationWarmupIsNotPersistedAsPermanentlyCompleted() {
        val navigator = mSourceRoot.resolve("ui/map/MapNotificationNavigator.kt").readText()
        val policy = mSourceRoot.resolve("notification/NotificationPermissionPolicy.kt").readText()

        assertFalse(navigator.contains("KEY_VENDOR_WARMUP_ATTEMPTED"))
        assertFalse(navigator.contains("hasVendorNotificationWarmupAttempted"))
        assertFalse(navigator.contains("markVendorNotificationWarmupAttempted"))
        assertFalse(policy.contains("hasWarmupAttempted"))
        assertTrue(navigator.contains("sVendorNotificationWarmupCompleted"))
        assertTrue(navigator.contains("hasProcessWarmupCompleted = sVendorNotificationWarmupCompleted"))
        assertTrue(navigator.contains("shouldWarmUpVendorNotificationsBeforePlay"))
    }

    private fun locateSourceRoot(): File {
        var dir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (true) {
            val sourceRoot = dir.resolve("src/main/java/com/bird/starryskysudoku")
            if (sourceRoot.isDirectory) return sourceRoot
            val appSourceRoot = dir.resolve("app/src/main/java/com/bird/starryskysudoku")
            if (appSourceRoot.isDirectory) return appSourceRoot
            dir = dir.parentFile ?: break
        }
        error("Unable to locate app source root")
    }
}
