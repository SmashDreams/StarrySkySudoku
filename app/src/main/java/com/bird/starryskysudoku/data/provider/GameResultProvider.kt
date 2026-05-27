package com.bird.starryskysudoku.data.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.data.entity.GameResultEntity

internal sealed class ResultQueryFilter {
    data object All : ResultQueryFilter()
    data class Username(val value: String) : ResultQueryFilter()
}

class GameResultProvider : ContentProvider() {

    companion object {
        /*
         * 只开放战绩集合和单条战绩两类地址，避免外部调用方访问未定义路径。
         */
        private const val MATCH_RESULTS = 1
        private const val MATCH_RESULT_ID = 2

        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(GameResultContract.AUTHORITY, GameResultContract.Results.PATH, MATCH_RESULTS)
            addURI(GameResultContract.AUTHORITY, "${GameResultContract.Results.PATH}/#", MATCH_RESULT_ID)
        }

        internal fun resolveResultsQueryFilter(
            selection: String?,
            selectionArgs: Array<out String>?
        ): ResultQueryFilter {
            if (selection == null) {
                if (!selectionArgs.isNullOrEmpty()) {
                    throw IllegalArgumentException("Selection arguments require a selection")
                }
                return ResultQueryFilter.All
            }
            if (selection != GameResultContract.Results.selectionForUsername()) {
                throw IllegalArgumentException("Unsupported results selection: $selection")
            }
            val username = selectionArgs?.firstOrNull()?.trim()
            if (username.isNullOrBlank()) {
                throw IllegalArgumentException("Username selection requires a nonblank argument")
            }
            return ResultQueryFilter.Username(username)
        }

        internal fun requireUnfilteredItemQuery(selection: String?, selectionArgs: Array<out String>?) {
            if (selection != null || !selectionArgs.isNullOrEmpty()) {
                throw IllegalArgumentException("Item uri queries do not support selection filters")
            }
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
         * 内容提供器对外暴露游标；数据库访问对象直接返回游标，
         * 避免手动搬运数据，保持本地数据库作为唯一数据源。
         */
        val appContext = requireNotNull(context?.applicationContext)
        val dao = DatabaseInitializer.getDatabase(appContext).gameResultDao()
        val cursor = when (sUriMatcher.match(uri)) {
            MATCH_RESULTS -> when (val filter = resolveResultsQueryFilter(selection, selectionArgs)) {
                ResultQueryFilter.All -> dao.queryAll()
                is ResultQueryFilter.Username -> dao.queryByUsername(filter.value)
            }
            MATCH_RESULT_ID -> {
                requireUnfilteredItemQuery(selection, selectionArgs)
                dao.queryById(ContentUris.parseId(uri))
            }
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
        /*
         * 清单文件中的写权限负责拦截普通外部应用。
         * 落库前的字段校验负责拦截同签名调用方传入的异常数据。
         */
        val id = DatabaseInitializer.getDatabase(appContext)
            .gameResultDao()
            .insert(GameResultEntity.fromContentValues(safeValues))
        val resultUri = ContentUris.withAppendedId(GameResultContract.Results.CONTENT_URI, id)
        appContext.contentResolver.notifyChange(GameResultContract.Results.CONTENT_URI, null)
        appContext.contentResolver.notifyChange(resultUri, null)
        return resultUri
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        if (sUriMatcher.match(uri) != MATCH_RESULT_ID) {
            throw IllegalArgumentException("Delete only supports item uri: $uri")
        }
        requireUnfilteredItemQuery(selection, selectionArgs)
        val appContext = requireNotNull(context?.applicationContext)
        val rows = DatabaseInitializer.getDatabase(appContext)
            .gameResultDao()
            .deleteById(ContentUris.parseId(uri))
        if (rows > 0) {
            /*
             * 同时通知集合地址和单条地址，确保外部观察者能刷新列表或详情。
             */
            appContext.contentResolver.notifyChange(GameResultContract.Results.CONTENT_URI, null)
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
