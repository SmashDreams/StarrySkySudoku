package com.bird.starryskysudoku.data.repository

import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.HistoryEntity
import com.bird.starryskysudoku.ui.play.BoardCell
import com.bird.starryskysudoku.ui.play.PlayBoardRules

class PlayRepository(private val mDb: AppDatabase) {
    // 棋盘数据、撤销历史和用户进度都在仓储层收口，页面不直接碰 DAO。
    private val mUserProgressRepository = UserProgressRepository(mDb)

    suspend fun loadBoard(levelNum: Int): Array<Array<BoardCell>>? {
        // 新开一局前先清掉该关卡遗留的历史记录，避免撤销读到旧局数据。
        mDb.historyDao().deleteForPass(levelNum)
        val values = mDb.problemDao().getValuesForLevel(levelNum)
        return PlayBoardRules.createBoard(values)
    }

    suspend fun recordHistory(history: HistoryEntity, passNum: Int, gameSession: String) {
        mDb.historyDao().insert(history)
        // 撤销历史只保留最近一段操作，避免单局记录无限增长。
        mDb.historyDao().trimToLimit(passNum, gameSession)
    }

    suspend fun undo(passNum: Int, gameSession: String): HistoryEntity? {
        // 撤销采用“取最新一条再删除”的方式，确保历史栈语义始终是后进先出。
        val history = mDb.historyDao().getLatest(passNum, gameSession) ?: return null
        mDb.historyDao().deleteById(history.mId)
        return history
    }

    suspend fun clearHistory(passNum: Int, gameSession: String) {
        // 重新开始或离开当前局时，只清这一局的历史，避免误删同关卡其它对局记录。
        mDb.historyDao().deleteForSession(passNum, gameSession)
    }

    suspend fun completePass(username: String, passNum: Int, nextPassNum: Int) {
        // 通关后的解锁、游玩次数和状态迁移统一委托给用户进度仓储处理。
        mUserProgressRepository.completePass(username, passNum, nextPassNum)
    }
}
