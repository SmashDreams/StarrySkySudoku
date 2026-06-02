package com.bird.starryskysudoku.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BgmMusicServiceStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun manifestRegistersBgmMusicService() {
        val manifest = locateAppRoot().resolve("src/main/AndroidManifest.xml").readText()

        assertTrue(manifest.contains(".media.BgmMusicService"))
        assertTrue(manifest.contains("android:exported=\"false\""))
    }

    @Test
    fun bgmServiceOwnsMediaPlayerAndBgmActions() {
        val service = mSourceRoot.resolve("media/BgmMusicService.kt")
        val source = service.readText()

        assertTrue(service.isFile)
        assertTrue(source.contains("class BgmMusicService : Service()"))
        assertTrue(source.contains("MediaPlayer"))
        assertTrue(source.contains("LocalBinder"))
        assertTrue(source.contains("fun playIfEnabled()"))
        assertTrue(source.contains("fun pause()"))
        assertTrue(source.contains("fun stopPlayback()"))
    }

    @Test
    fun playMusicNoLongerOwnsBackgroundMediaPlayer() {
        val source = mSourceRoot.resolve("media/PlayMusic.kt").readText()

        assertFalse(source.contains("MediaPlayer"))
        assertFalse(source.contains("playBGM()"))
        assertFalse(source.contains("stopBGM()"))
    }

    @Test
    fun applicationUsesForegroundBgmController() {
        val app = mSourceRoot.resolve("StarrySkySudokuApp.kt").readText()
        val controller = mSourceRoot.resolve("media/AppForegroundBgmController.kt")

        assertTrue(controller.isFile)
        assertTrue(app.contains("AppForegroundBgmController"))
        assertTrue(app.contains("registerActivityLifecycleCallbacks"))
    }

    @Test
    fun activitiesDoNotDriveBgmFromPauseResume() {
        val activitySources = listOf(
            mSourceRoot.resolve("ui/map/MapActivity.kt"),
            mSourceRoot.resolve("ui/guide/GuideActivity.kt"),
            mSourceRoot.resolve("ui/howtoplay/HowToPlayActivity.kt"),
            mSourceRoot.resolve("ui/play/PlayNavigationController.kt")
        ).joinToString("\n") { it.readText() }

        assertFalse(activitySources.contains("playBGM"))
        assertFalse(activitySources.contains("stopBGM"))
    }

    @Test
    fun foregroundControllerBindsServiceInsteadOfStartingItFromLifecycleCallbacks() {
        val foregroundController = mSourceRoot.resolve("media/AppForegroundBgmController.kt").readText()
        val musicController = mSourceRoot.resolve("media/BgmMusicController.kt").readText()

        assertTrue(foregroundController.contains("bind("))
        assertTrue(foregroundController.contains("unbind("))
        assertFalse(foregroundController.contains("playIfEnabled(mApplication)"))
        assertFalse(musicController.contains("startService"))
    }

    private fun locateAppRoot(): File {
        var dir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (true) {
            if (dir.resolve("src/main/AndroidManifest.xml").isFile) return dir
            if (dir.resolve("app/src/main/AndroidManifest.xml").isFile) return dir.resolve("app")
            dir = dir.parentFile ?: break
        }
        error("Unable to locate app root")
    }

    private fun locateSourceRoot(): File {
        val appRoot = locateAppRoot()
        return appRoot.resolve("src/main/java/com/bird/starryskysudoku")
    }
}
