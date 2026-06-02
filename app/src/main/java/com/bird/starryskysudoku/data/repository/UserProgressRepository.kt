package com.bird.starryskysudoku.data.repository

import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.UserMapEntity

object PassStatus {
    // 地图页目前只区分三种进度状态，供适配器决定星星和路径样式。
    const val COMPLETED = "已通关"
    const val TODO = "待通关"
    const val LOCKED = "未通关"
}

class UserProgressRepository(private val mDb: AppDatabase) {
    companion object {
        private const val MAX_LEVEL = 40
    }

    suspend fun ensureUserMap(username: String): String {
        val safeUsername = username.trim().ifEmpty { LauncherSessionReader.GUEST_USERNAME }
        if (mDb.userMapDao().getAllForUser(safeUsername).isNotEmpty()) return safeUsername

        // 首次进入地图的用户自动生成完整四十关进度，第一关默认可玩。
        val userRows = (1..MAX_LEVEL).map { passNum ->
            UserMapEntity(
                mUsername = safeUsername,
                mPassNum = passNum,
                mStatus = if (passNum == 1) PassStatus.TODO else PassStatus.LOCKED,
                mPlayTime = 0
            )
        }
        mDb.userMapDao().insertAll(userRows)
        return safeUsername
    }

    suspend fun completePass(username: String, passNum: Int, nextPassNum: Int) {
        val safeUsername = ensureUserMap(username)
        val userMapDao = mDb.userMapDao()
        // 通关后同时更新当前关状态、游玩次数，并在需要时解锁下一关。
        userMapDao.updateStatus(safeUsername, passNum, PassStatus.COMPLETED)
        val map = userMapDao.getByUserAndPass(safeUsername, passNum)
        val newTimes = (map?.mPlayTime ?: 0) + 1
        userMapDao.updatePlayTime(safeUsername, passNum, newTimes)
        val nextMap = userMapDao.getByUserAndPass(safeUsername, nextPassNum)
        if (passNum < MAX_LEVEL && nextMap?.mStatus != PassStatus.COMPLETED) {
            userMapDao.updateStatus(safeUsername, nextPassNum, PassStatus.TODO)
        }
    }
}
