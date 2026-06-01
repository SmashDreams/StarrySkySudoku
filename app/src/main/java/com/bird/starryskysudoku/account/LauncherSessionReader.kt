package com.bird.starryskysudoku.account

import android.content.ContentResolver
import android.database.Cursor
import android.util.Log
import com.bird.starrysky.contracts.SharedSessionContract

object LauncherSessionReader {
    const val GUEST_USERNAME = SharedSessionContract.GUEST_USERNAME
    private const val TAG = "LauncherSessionReader"

    fun readUsername(contentResolver: ContentResolver): String {
        return try {
            contentResolver.query(
                LauncherSessionContract.Session.CONTENT_URI,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return GUEST_USERNAME

                val loggedIn = cursor.readBoolean(LauncherSessionContract.Session.COLUMN_LOGGED_IN)
                val username = cursor.readString(LauncherSessionContract.Session.COLUMN_USERNAME)
                if (loggedIn && username.isNotBlank()) username else GUEST_USERNAME
            } ?: GUEST_USERNAME
        } catch (e: RuntimeException) {
            Log.w(TAG, "Unable to read launcher session; using guest", e)
            GUEST_USERNAME
        }
    }

    private fun Cursor.readString(columnName: String): String {
        val index = getColumnIndex(columnName)
        return if (index >= 0) getString(index).orEmpty().trim() else ""
    }

    private fun Cursor.readBoolean(columnName: String): Boolean {
        val index = getColumnIndex(columnName)
        if (index < 0) return false
        return when (getString(index)?.lowercase()) {
            "1", "true" -> true
            else -> false
        }
    }
}
