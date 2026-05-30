package com.bird.starryskysudoku.ui.play

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class PlayRouteTest {
    @Test
    fun createAddsLevelAndUsernameExtras() {
        val intent = PlayRoute.create(RuntimeEnvironment.getApplication(), 7, "alice")

        assertEquals("7", intent.getStringExtra(PlayRoute.EXTRA_LEVEL))
        assertEquals("alice", intent.getStringExtra(PlayRoute.EXTRA_USERNAME))
    }

    @Test
    fun readLevelAcceptsCurrentAndLegacyLevelExtras() {
        assertEquals(5, PlayRoute.readLevel(Intent().putExtra(PlayRoute.EXTRA_LEVEL, "5")))
        assertEquals(6, PlayRoute.readLevel(Intent().putExtra(PlayRoute.EXTRA_LEGACY_LEVEL, "6")))
    }

    @Test
    fun readLevelFallsBackToOneWhenInputIsInvalid() {
        assertEquals(1, PlayRoute.readLevel(Intent().putExtra(PlayRoute.EXTRA_LEVEL, "50")))
        assertEquals(1, PlayRoute.readLevel(Intent().putExtra(PlayRoute.EXTRA_LEVEL, "bad")))
    }

    @Test
    fun readUsernameReturnsNonblankUsernameOnly() {
        assertEquals("alice", PlayRoute.readUsername(Intent().putExtra(PlayRoute.EXTRA_USERNAME, "alice")))
        assertEquals(null, PlayRoute.readUsername(Intent().putExtra(PlayRoute.EXTRA_USERNAME, " ")))
    }
}
