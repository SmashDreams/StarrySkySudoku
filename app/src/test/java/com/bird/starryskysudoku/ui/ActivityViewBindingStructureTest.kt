package com.bird.starryskysudoku.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ActivityViewBindingStructureTest {
    @Test
    fun playActivityUsesViewBindingForRootLayout() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/play/PlayActivity.kt").readText()

        assertTrue(source.contains("ActivityPlayBinding"))
        assertTrue(source.contains("setContentView(mBinding.root)"))
        assertFalse(source.contains("setContentView(R.layout.activity_play)"))
    }

    @Test
    fun mapActivityUsesViewBindingForRootLayout() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/map/MapActivity.kt").readText()

        assertTrue(source.contains("ActivityMappageBinding"))
        assertTrue(source.contains("setContentView(mBinding.root)"))
        assertFalse(source.contains("setContentView(R.layout.activity_mappage)"))
    }
    @Test
    fun guideActivityUsesViewBindingForRootLayout() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/guide/GuideActivity.kt").readText()

        assertTrue(source.contains("ActivityGuidepageBinding"))
        assertTrue(source.contains("setContentView(mBinding.root)"))
        assertFalse(source.contains("setContentView(R.layout.activity_guidepage)"))
    }

    @Test
    fun howToPlayActivityUsesViewBindingForRootLayout() {
        val source = File("src/main/java/com/bird/starryskysudoku/ui/howtoplay/HowToPlayActivity.kt").readText()

        assertTrue(source.contains("ActivityHowtoplaypageBinding"))
        assertTrue(source.contains("setContentView(mBinding.root)"))
        assertFalse(source.contains("setContentView(R.layout.activity_howtoplaypage)"))
    }

}
