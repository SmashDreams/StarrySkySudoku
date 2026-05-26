package com.bird.starryskysudoku.account

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherSessionContractTest {
    @Test fun launcherSessionUriUsesStableAuthorityAndPath() {
        assertEquals("com.bird.launcher.provider", LauncherSessionContract.AUTHORITY)
        assertEquals("session", LauncherSessionContract.Session.PATH)
        assertEquals("content://com.bird.launcher.provider/session", LauncherSessionContract.Session.CONTENT_URI_STRING)
        assertEquals("username", LauncherSessionContract.Session.COLUMN_USERNAME)
        assertEquals("logged_in", LauncherSessionContract.Session.COLUMN_LOGGED_IN)
        assertEquals("guest", LauncherSessionReader.GUEST_USERNAME)
    }
}
