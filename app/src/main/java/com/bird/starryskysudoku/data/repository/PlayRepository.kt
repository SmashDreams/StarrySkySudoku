package com.bird.starryskysudoku.data.repository

import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.HistoryEntity
import com.bird.starryskysudoku.ui.play.BoardCell
import com.bird.starryskysudoku.ui.play.PlayBoardRules

class PlayRepository(private val mDb: AppDatabase) {
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
        val history = mDb.historyDao().getLatest(passNum, gameSession) ?: return null
        mDb.historyDao().deleteById(history.mId)
        return history
    }

    suspend fun clearHistory(passNum: Int, gameSession: String) {
        mDb.historyDao().deleteForSession(passNum, gameSession)
    }

    suspend fun completePass(username: String, passNum: Int, nextPassNum: Int) {
        mUserProgressRepository.completePass(username, passNum, nextPassNum)
    }
}
