package com.bird.starryskysudoku.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RouteUsageStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun entryAndGuideUseMapRouteForMapNavigation() {
        val appEntry = mSourceRoot.resolve("ui/splash/AppEntryActivity.kt").readText()
        val guide = mSourceRoot.resolve("ui/guide/GuideActivity.kt").readText()

        assertTrue(appEntry.contains("MapRoute.create"))
        assertTrue(guide.contains("MapRoute.create"))
        assertFalse(appEntry.contains("MapActivity.EXTRA_FLASH_HOME"))
        assertFalse(guide.contains("MapActivity.EXTRA_FLASH_HOME"))
    }

    @Test
    fun countdownServiceUsesPlayRouteForNotificationIntent() {
        val service = mSourceRoot.resolve("timer/CountdownTimerService.kt").readText()

        assertTrue(service.contains("PlayRoute.create"))
        assertFalse(service.contains("""putExtra("mNum""""))
    }

    @Test
    fun activityCompatibilityConstantsDelegateToRoutes() {
        val playActivity = mSourceRoot.resolve("ui/play/PlayActivity.kt").readText()
        val mapActivity = mSourceRoot.resolve("ui/map/MapActivity.kt").readText()

        assertTrue(playActivity.contains("const val EXTRA_USERNAME = PlayRoute.EXTRA_USERNAME"))
        assertTrue(mapActivity.contains("const val EXTRA_FLASH_HOME = MapRoute.EXTRA_FLASH_HOME"))
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
