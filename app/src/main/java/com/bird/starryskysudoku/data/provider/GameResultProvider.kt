package com.bird.starryskysudoku.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.bird.starryskysudoku.data.dao.GameResultDao
import com.bird.starryskysudoku.data.database.DatabaseInitializer

class GameResultProvider : ContentProvider() {

    companion object {
        private const val MATCH_RESULTS = 1
        private const val MATCH_RESULT_ID = 2

        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(GameResultContract.AUTHORITY, GameResultContract.Results.PATH, MATCH_RESULTS)
            addURI(GameResultContract.AUTHORITY, "${GameResultContract.Results.PATH}/#", MATCH_RESULT_ID)
        }
    }

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String {
        return when (sUriMatcher.match(uri)) {
            MATCH_RESULTS -> GameResultContract.Results.CONTENT_TYPE
            MATCH_RESULT_ID -> GameResultContract.Results.CONTENT_ITEM_TYPE
            else -> throw IllegalArgumentException("不支持的 uri：$uri")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val appContext = requireNotNull(context?.applicationContext)
        val dao = DatabaseInitializer.getDatabase(appContext).gameResultDao()
        val cursor = when (sUriMatcher.match(uri)) {
            MATCH_RESULTS -> queryResults(dao, selection, selectionArgs)
            MATCH_RESULT_ID -> dao.queryById(ContentUris.parseId(uri))
            else -> throw IllegalArgumentException("不支持的 uri：$uri")
        }
        cursor.setNotificationUri(appContext.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: android.content.ContentValues?): Uri {
        throw UnsupportedOperationException("GameResultProvider is read-only: $uri")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("GameResultProvider is read-only: $uri")
    }

    override fun update(
        uri: Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("GameResultProvider is read-only: $uri")
    }

    private fun queryResults(
        dao: GameResultDao,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Cursor {
        val username = selectionArgs
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { selection == GameResultContract.Results.selectionForUsername() && it.isNotEmpty() }
        return if (username == null) dao.queryAll() else dao.queryByUsername(username)
    }
}
