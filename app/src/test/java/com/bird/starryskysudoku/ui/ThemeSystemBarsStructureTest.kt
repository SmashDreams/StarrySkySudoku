package com.bird.starryskysudoku.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeSystemBarsStructureTest {
    private val mProjectRoot = locateProjectRoot()

    @Test
    fun appThemeUsesNightSkySystemBarsInsteadOfDefaultPurple() {
        val themes = mProjectRoot.resolve("app/src/main/res/values/themes.xml").readText()
        val nightThemes = mProjectRoot.resolve("app/src/main/res/values-night/themes.xml").readText()
        val colors = mProjectRoot.resolve("app/src/main/res/values/colors.xml").readText()

        assertTrue(colors.contains("name=\"system_bar_night_sky\""))
        assertTrue(themes.contains("<item name=\"android:statusBarColor\">@color/system_bar_night_sky</item>"))
        assertTrue(themes.contains("<item name=\"android:navigationBarColor\">@color/system_bar_night_sky</item>"))
        assertTrue(themes.contains("<item name=\"android:windowLightStatusBar\">false</item>"))
        assertTrue(themes.contains("<item name=\"android:windowLightNavigationBar\">false</item>"))
        assertTrue(nightThemes.contains("<item name=\"android:statusBarColor\">@color/system_bar_night_sky</item>"))
        assertTrue(nightThemes.contains("<item name=\"android:navigationBarColor\">@color/system_bar_night_sky</item>"))
    }

    private fun locateProjectRoot(): File {
        var dir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (true) {
            if (dir.resolve("app/src/main/res/values/themes.xml").isFile) return dir
            if (dir.resolve("src/main/res/values/themes.xml").isFile) return dir.parentFile ?: dir
            dir = dir.parentFile ?: break
        }
        error("Unable to locate project root")
    }
}
