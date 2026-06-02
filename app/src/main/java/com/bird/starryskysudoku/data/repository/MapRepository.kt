package com.bird.starryskysudoku.data.repository

import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.entity.UserMapEntity

class MapRepository(private val mDb: AppDatabase) {
    private val mUserProgressRepository = UserProgressRepository(mDb)

    suspend fun loadMapData(username: String): List<Array<MapEntity?>> {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        // 地图页按每行四关组织数据，直接转换成适配器当前使用的二维结构。
        return mDb.userMapDao().getAllForUser(safeUsername)
            .map { it.toMapEntity() }
            .chunked(4)
            .map { chunk ->
                arrayOf(
                    chunk.getOrNull(0), chunk.getOrNull(1),
                    chunk.getOrNull(2), chunk.getOrNull(3)
                )
            }
    }

    suspend fun getPassStatus(username: String, passNum: Int): String? {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        return mDb.userMapDao().getByUserAndPass(safeUsername, passNum)?.mStatus
    }

    suspend fun getPassTimes(username: String, passNum: Int): String {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        return (mDb.userMapDao().getByUserAndPass(safeUsername, passNum)?.mPlayTime ?: 0).toString()
    }

    suspend fun updateStatus(username: String, passNum: Int, status: String) {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        mDb.userMapDao().updateStatus(safeUsername, passNum, status)
    }

    suspend fun incrementPlayTime(username: String, passNum: Int) {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        val map = mDb.userMapDao().getByUserAndPass(safeUsername, passNum)
        mDb.userMapDao().updatePlayTime(safeUsername, passNum, (map?.mPlayTime ?: 0) + 1)
    }

    // 仓储层在这里完成数据库实体到页面实体的转换，避免页面层直接感知表结构。
    private fun UserMapEntity.toMapEntity() = MapEntity(
        mPassNum = mPassNum,
        mStatus = mStatus,
        mPlayTime = mPlayTime
    )
}
