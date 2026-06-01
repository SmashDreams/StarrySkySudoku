package com.bird.starryskysudoku.ui.map

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapActivityControllerStructureTest {
    private val mSourceRoot = File("src/main/java/com/bird/starryskysudoku/ui/map")

    @Test
    fun mapActivityDelegatesLargeResponsibilitiesToFocusedControllers() {
        val mapActivity = mSourceRoot.resolve("MapActivity.kt").readText()

        assertTrue(mSourceRoot.resolve("MapPassDialogController.kt").isFile)
        assertTrue(mSourceRoot.resolve("MapSettingsController.kt").isFile)
        assertTrue(mSourceRoot.resolve("MapNotificationNavigator.kt").isFile)
        assertTrue(mapActivity.contains("MapPassDialogController("))
        assertTrue(mapActivity.contains("MapSettingsController("))
        assertTrue(mapActivity.contains("MapNotificationNavigator("))
    }

    @Test
    fun mapActivityNoLongerOwnsDialogSettingsAndNotificationDetails() {
        val mapActivity = mSourceRoot.resolve("MapActivity.kt").readText()

        assertFalse(mapActivity.contains("DialogPasscheckBinding"))
        assertFalse(mapActivity.contains("DialogSettingsBinding"))
        assertFalse(mapActivity.contains("NotificationCompat"))
        assertFalse(mapActivity.contains("NotificationChannel"))
        assertFalse(mapActivity.contains("AppCompatDelegate"))
    }
}
