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
            // 对外只接受“按用户名过滤”这一种查询条件，减少提供器暴露面的复杂度。
            if (selection == null) {
                if (!selectionArgs.isNullOrEmpty()) {
                    throw IllegalArgumentException("selectionArgs 不能脱离 selection 单独传入")
                }
                return ResultQueryFilter.All
            }
            if (selection != GameResultContract.Results.selectionForUsername()) {
                throw IllegalArgumentException("不支持的 results selection：$selection")
            }
            if (selectionArgs?.size != 1) {
                throw IllegalArgumentException("按用户名筛选时必须且只能传入一个参数")
            }
            val username = selectionArgs[0].trim()
            if (username.isBlank()) {
                throw IllegalArgumentException("按用户名筛选时参数不能为空白")
            }
            return ResultQueryFilter.Username(username)
        }

        internal fun requireSupportedSortOrder(sortOrder: String?) {
            // 查询排序固定只支持“最新优先”，避免外部随意传参造成结果口径不一致。
            if (sortOrder != null && sortOrder != GameResultContract.Results.SORT_NEWEST_FIRST) {
                throw IllegalArgumentException("不支持的 results 排序方式：$sortOrder")
            }
        }

        internal fun requireUnfilteredItemQuery(selection: String?, selectionArgs: Array<out String>?) {
            // 单条地址已经唯一定位到记录本身，不再额外接受筛选条件。
            if (selection != null || !selectionArgs.isNullOrEmpty()) {
                throw IllegalArgumentException("单条 item uri 查询不支持额外 selection 过滤")
            }
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
        /*
         * 内容提供器对外暴露游标；数据库访问对象直接返回游标，
         * 避免手动搬运数据，保持本地数据库作为唯一数据源。
         */
        val appContext = requireNotNull(context?.applicationContext)
        val dao = DatabaseInitializer.getDatabase(appContext).gameResultDao()
        val cursor = when (sUriMatcher.match(uri)) {
            MATCH_RESULTS -> {
                requireSupportedSortOrder(sortOrder)
                when (val filter = resolveResultsQueryFilter(selection, selectionArgs)) {
                    ResultQueryFilter.All -> dao.queryAll()
                    is ResultQueryFilter.Username -> dao.queryByUsername(filter.value)
                }
            }
            MATCH_RESULT_ID -> {
                requireUnfilteredItemQuery(selection, selectionArgs)
                dao.queryById(ContentUris.parseId(uri))
            }
            else -> throw IllegalArgumentException("不支持的 uri：$uri")
        }
        cursor.setNotificationUri(appContext.contentResolver, uri)
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        if (sUriMatcher.match(uri) != MATCH_RESULTS) {
            throw IllegalArgumentException("insert 只支持 collection uri：$uri")
        }
        // 落库前先拦截空参数，避免后续字段校验报错时丢失根因。
        val safeValues = values ?: throw IllegalArgumentException("ContentValues 不能为空")
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
            throw IllegalArgumentException("delete 只支持 item uri：$uri")
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
    ): Int {
        // 战绩只允许新增、查询和按条删除，不开放更新，避免外部篡改既有结果。
        return 0
    }
}
