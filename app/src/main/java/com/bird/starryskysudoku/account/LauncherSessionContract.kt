package com.bird.starryskysudoku.account

import android.net.Uri
import com.bird.starrysky.contracts.SharedSessionContract

object LauncherSessionContract {
    const val AUTHORITY = SharedSessionContract.AUTHORITY
    const val READ_PERMISSION = SharedSessionContract.READ_PERMISSION
    const val CONTENT_URI_BASE = SharedSessionContract.CONTENT_URI_BASE

    object Session {
        const val PATH = SharedSessionContract.Session.PATH
        const val CONTENT_URI_STRING = SharedSessionContract.Session.CONTENT_URI_STRING
        val CONTENT_URI: Uri by lazy { SharedSessionContract.Session.CONTENT_URI }
        const val COLUMN_USERNAME = SharedSessionContract.Session.COLUMN_USERNAME
        const val COLUMN_LOGGED_IN = SharedSessionContract.Session.COLUMN_LOGGED_IN
    }
}
