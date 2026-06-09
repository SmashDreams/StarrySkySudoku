package com.bird.starryskysudoku.data.repository

import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.entity.UserMapEntity

class MapRepository(private val mDb: AppDatabase) {
    // 地图仓储负责保证“用户进度存在”与“页面展示结构可用”这两件事同时成立。
    private val mUserProgressRepository = UserProgressRepository(mDb)

    suspend fun loadMapData(username: String): List<Array<MapEntity?>> {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        // 关卡数据按每行四关组织：列表首元素是最高关卡行（界面最上方），末元素是第一关行（界面最下方）。
        return mDb.userMapDao().getAllForUser(safeUsername)
            .map { it.toMapEntity() }
            .chunked(4)
            .map { chunk ->
                arrayOf(
                    chunk.getOrNull(0), chunk.getOrNull(1),
                    chunk.getOrNull(2), chunk.getOrNull(3)
                )
            }
            .reversed()
    }

    suspend fun getPassStatus(username: String, passNum: Int): String? {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        // 查询前先确保该用户的整张地图已补齐，避免新用户直接读到空结果。
        return mDb.userMapDao().getByUserAndPass(safeUsername, passNum)?.mStatus
    }

    suspend fun getPassTimes(username: String, passNum: Int): String {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        // 弹窗文案直接使用字符串次数，仓储层顺手把数据库整数转成展示格式。
        return (mDb.userMapDao().getByUserAndPass(safeUsername, passNum)?.mPlayTime ?: 0).toString()
    }

    suspend fun updateStatus(username: String, passNum: Int, status: String) {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        mDb.userMapDao().updateStatus(safeUsername, passNum, status)
    }

    suspend fun incrementPlayTime(username: String, passNum: Int) {
        val safeUsername = mUserProgressRepository.ensureUserMap(username)
        val map = mDb.userMapDao().getByUserAndPass(safeUsername, passNum)
        // 游玩次数按读取旧值再加一的方式更新，兼容首次进入该关时还没有历史次数的情况。
        mDb.userMapDao().updatePlayTime(safeUsername, passNum, (map?.mPlayTime ?: 0) + 1)
    }

    // 仓储层在这里完成数据库实体到页面实体的转换，避免页面层直接感知表结构。
    private fun UserMapEntity.toMapEntity() = MapEntity(
        mPassNum = mPassNum,
        mStatus = mStatus,
        mPlayTime = mPlayTime
    )
}
