package com.bird.starryskysudoku.data.repository

import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.UserMapEntity

object PassStatus {
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
