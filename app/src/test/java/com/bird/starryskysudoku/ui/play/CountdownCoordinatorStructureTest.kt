package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CountdownCoordinatorStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playActivityDelegatesCountdownServiceAndReceiverWork() {
        val activity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val coordinator = mSourceRoot.resolve("ui/play/CountdownCoordinator.kt")

        assertTrue(coordinator.isFile)
        assertTrue(activity.contains("CountdownCoordinator("))
        assertTrue(activity.contains("mCountdownCoordinator.onStart()"))
        assertTrue(activity.contains("mCountdownCoordinator.onStop()"))
        assertTrue(activity.contains("mCountdownCoordinator.start()"))
        assertTrue(activity.contains("mCountdownCoordinator.stop()"))
        assertFalse(activity.contains("BroadcastReceiver"))
        assertFalse(activity.contains("registerReceiver("))
        assertFalse(activity.contains("startForegroundService("))
    }

    @Test
    fun countdownCoordinatorOwnsTimerServiceContractUsage() {
        val coordinator = mSourceRoot.resolve("ui/play/CountdownCoordinator.kt").readText()

        assertTrue(coordinator.contains("CountdownTimerContract.ACTION_COUNTDOWN_TICK"))
        assertTrue(coordinator.contains("CountdownTimerService::class.java"))
        assertTrue(coordinator.contains("ContextCompat.registerReceiver"))
        assertTrue(coordinator.contains("startService(serviceIntent)"))
        assertFalse(coordinator.contains("startForegroundService"))
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
