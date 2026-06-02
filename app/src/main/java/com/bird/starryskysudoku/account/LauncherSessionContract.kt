package com.bird.starryskysudoku.account

import android.net.Uri
import com.bird.starrysky.contracts.SharedSessionContract

object LauncherSessionContract {
    // 直接复用茶馆应用暴露的会话契约，避免本项目自己维护一份重复定义。
    const val AUTHORITY = SharedSessionContract.AUTHORITY
    const val READ_PERMISSION = SharedSessionContract.READ_PERMISSION
    const val CONTENT_URI_BASE = SharedSessionContract.CONTENT_URI_BASE

    object Session {
        // 这里只暴露本项目真正会读到的字段，使用方不需要关心共享契约的全部细节。
        const val PATH = SharedSessionContract.Session.PATH
        const val CONTENT_URI_STRING = SharedSessionContract.Session.CONTENT_URI_STRING
        val CONTENT_URI: Uri by lazy { SharedSessionContract.Session.CONTENT_URI }
        const val COLUMN_USERNAME = SharedSessionContract.Session.COLUMN_USERNAME
        const val COLUMN_LOGGED_IN = SharedSessionContract.Session.COLUMN_LOGGED_IN
    }
}
