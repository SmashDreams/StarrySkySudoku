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

    @Test
    fun mapActivityPositionsListToCurrentPassWithoutExtraAnimatedScrollOnDataLoad() {
        val mapActivity = mSourceRoot.resolve("MapActivity.kt").readText()
        val observerBody = mapActivity.substringAfter("mViewModel.mMapData.observe")
            .substringBefore("mAdapter.setOpenListener")

        assertTrue(observerBody.contains("positionMapToCurrentPass()"))
        assertFalse(observerBody.contains("scrollToPosition(mAdapter.getPosition())"))
        assertFalse(observerBody.contains("smoothScrollBy"))

        assertTrue(mapActivity.contains("scrollToPositionWithOffset"))
        assertTrue(mapActivity.contains("MAP_PASS_ROW_HEIGHT_DP"))
        assertTrue(mapActivity.contains("private const val MAP_PASS_ROW_HEIGHT_DP = 340"))
        assertTrue(mapActivity.contains("MAP_STAR_VISUAL_HEIGHT_DP"))
        assertTrue(mapActivity.contains("MAP_BOTTOM_STAR_REVEAL_DP"))
        assertFalse(mapActivity.contains("setPadding(0, 0, 0"))
        assertTrue(mapActivity.contains("getCurrentProgressOffsetDp()"))
        assertTrue(mapActivity.contains("hasPendingWinNavigation"))
        assertTrue(mapActivity.contains("pendingCompletedLevel"))
        assertTrue(mapActivity.contains("getPositionForLevel"))
        assertTrue(mapActivity.contains("getTopOffsetDpForLevel"))
        assertTrue(mapActivity.contains("scrollMapAfterCompletedLevel"))
        assertTrue(mapActivity.contains("restorePendingReturnAnchor()"))
        assertTrue(mapActivity.contains("MapRoute.putReturnAnchor"))
        val completionScrollBody = mapActivity.substringAfter("private fun scrollMapAfterCompletedLevel")
            .substringBefore("private fun dpToPx")
        assertFalse(completionScrollBody.contains("positionMapToCurrentPass"))
    }

    @Test
    fun mapBlocksInteractionWhileWaitingForAutomaticNextLevelDialog() {
        val mapActivity = mSourceRoot.resolve("MapActivity.kt").readText()
        val passDialog = mSourceRoot.resolve("MapPassDialogController.kt").readText()

        assertTrue(mapActivity.contains("mSetMapInteractionEnabled"))
        assertTrue(mapActivity.contains("setMapInteractionEnabled(enabled)"))
        assertTrue(mapActivity.contains("mRecyclerView.isEnabled = enabled"))
        assertTrue(mapActivity.contains("mSettings.isEnabled = enabled"))
        assertTrue(passDialog.contains("setMapInteractionEnabled(false)"))
        assertTrue(passDialog.contains("if (mInteractionLocked) return"))
        assertTrue(passDialog.contains("setMapInteractionEnabled(true)"))
    }
}
