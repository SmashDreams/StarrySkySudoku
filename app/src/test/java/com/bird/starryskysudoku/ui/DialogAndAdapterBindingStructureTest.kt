package com.bird.starryskysudoku.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DialogAndAdapterBindingStructureTest {
    @Test
    fun passListAdapterUsesItemViewBinding() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/map/PassListAdapter.kt").readText()

        assertTrue(source.contains("PassItemBinding"))
        assertTrue(source.contains("PassFirstItemBinding"))
        assertFalse(source.contains("itemView.findViewById"))
    }

    @Test
    fun mapControllersUseDialogBindingsForSettingsAndPassCheck() {
        val passDialogController = File("src/main/java/com/bird/starryskysudoku/ui/map/MapPassDialogController.kt").readText()
        val settingsController = File("src/main/java/com/bird/starryskysudoku/ui/map/MapSettingsController.kt").readText()

        assertTrue(passDialogController.contains("DialogPasscheckBinding"))
        assertTrue(settingsController.contains("DialogSettingsBinding"))
        assertTrue(settingsController.contains("MapRoute.clearReturnAnchor(mActivity.intent)"))
        assertTrue(settingsController.contains("hideImmediately(mSettingsDialog)"))
        assertFalse(settingsController.contains("mRestartAfterLanguageChange()"))
        assertTrue(settingsController.contains("readEffectiveLanguage()"))
        assertTrue(settingsController.contains("AppLocaleContext.applyLanguageToCurrentResources"))
        assertTrue(settingsController.contains("refreshSettingsTexts(settingsBinding)"))
        assertTrue(settingsController.contains("mOnLanguageChanged()"))
        assertFalse(settingsController.contains("mSettingsDialog.findViewById"))
        assertFalse(passDialogController.contains("findViewById<TextView>(R.id.passcheck"))
        assertFalse(passDialogController.contains("findViewById<ImageView>(R.id.passcheck"))
    }

    @Test
    fun playActivityUsesDialogBindingsForPauseWinAndLose() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/play/PlayDialogController.kt").readText()

        assertTrue(source.contains("DialogPauseBinding"))
        assertTrue(source.contains("DialogWinBinding"))
        assertTrue(source.contains("DialogLoseBinding"))
        assertFalse(source.contains("mPauseDialog.findViewById"))
        assertFalse(source.contains("mWinDialog.findViewById"))
        assertFalse(source.contains("mLoseDialog.findViewById"))
    }
}
