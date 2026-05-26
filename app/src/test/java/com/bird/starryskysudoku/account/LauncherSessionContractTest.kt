package com.bird.starryskysudoku.account

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherSessionContractTest {
    @Test fun launcherSessionUriUsesStableAuthorityAndPath() {
        assertEquals("com.bird.launcher.provider", LauncherSessionContract.AUTHORITY)
        assertEquals("session", LauncherSessionContract.Session.PATH)
        assertEquals("content://com.bird.launcher.provider/session", LauncherSessionContract.Session.CONTENT_URI_STRING)
        assertEquals(Uri.parse("content://com.bird.launcher.provider/session"), LauncherSessionContract.Session.CONTENT_URI)
        assertEquals("username", LauncherSessionContract.Session.COLUMN_USERNAME)
        assertEquals("logged_in", LauncherSessionContract.Session.COLUMN_LOGGED_IN)
        assertEquals("guest", LauncherSessionReader.GUEST_USERNAME)
    }
}
