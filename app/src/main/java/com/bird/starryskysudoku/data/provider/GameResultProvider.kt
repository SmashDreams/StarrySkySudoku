package com.bird.starryskysudoku.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.data.entity.GameResultEntity

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
            else -> throw IllegalArgumentException("Unsupported uri: $uri")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        /*
         * ContentProvider 对外暴露 Cursor；Room DAO 直接返回 Cursor，
         * 避免手动 MatrixCursor 搬运数据，保持 Room 作为唯一数据源。
         */
        val appContext = requireNotNull(context?.applicationContext)
        val dao = DatabaseInitializer.getDatabase(appContext).gameResultDao()
        val cursor = when (sUriMatcher.match(uri)) {
            MATCH_RESULTS -> dao.queryAll()
            MATCH_RESULT_ID -> dao.queryById(ContentUris.parseId(uri))
            else -> throw IllegalArgumentException("Unsupported uri: $uri")
        }
        cursor.setNotificationUri(appContext.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        if (sUriMatcher.match(uri) != MATCH_RESULTS) {
            throw IllegalArgumentException("Insert only supports collection uri: $uri")
        }
        val safeValues = values ?: throw IllegalArgumentException("ContentValues must not be null")
        val appContext = requireNotNull(context?.applicationContext)
        val id = DatabaseInitializer.getDatabase(appContext)
            .gameResultDao()
            .insert(GameResultEntity.fromContentValues(safeValues))
        val resultUri = ContentUris.withAppendedId(GameResultContract.Results.CONTENT_URI, id)
        appContext.contentResolver.notifyChange(resultUri, null)
        return resultUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (sUriMatcher.match(uri) != MATCH_RESULT_ID) {
            throw IllegalArgumentException("Delete only supports item uri: $uri")
        }
        val appContext = requireNotNull(context?.applicationContext)
        val rows = DatabaseInitializer.getDatabase(appContext)
            .gameResultDao()
            .deleteById(ContentUris.parseId(uri))
        if (rows > 0) {
            appContext.contentResolver.notifyChange(uri, null)
        }
        return rows
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
