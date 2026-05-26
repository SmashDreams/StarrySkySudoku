package com.bird.starryskysudoku.ui.play

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.HistoryEntity
import com.bird.starryskysudoku.data.entity.UserMapEntity
import com.bird.starryskysudoku.timer.CountdownTimerContract
import kotlinx.coroutines.launch
import java.util.UUID

class PlayViewModel(private val mDb: AppDatabase) : ViewModel() {

    companion object {
        const val SELECT_NONE = 0
        const val SELECT_ON = 1
        const val BE_SELECTED = -1
        const val WRONG = 2
        const val PROBLEM = 1
        const val EMPTY = 0
        const val TYPE_NUMBER = 0
        const val TYPE_TAG = 1
    }

    data class CellData(
        var mRow: Int,
        var mCol: Int,
        var mValue: String,
        var mBlock: Int,
        var mStatus: Int = SELECT_NONE,
        var mType: Int = EMPTY
    )

    private val mBoardSource = MutableLiveData<Array<Array<CellData>>>()
    val mBoard: LiveData<Array<Array<CellData>>> = mBoardSource

    private val mHasWonSource = MutableLiveData(false)
    val mHasWon: LiveData<Boolean> = mHasWonSource

    private val mIsWrongSource = MutableLiveData(false)
    val mIsWrong: LiveData<Boolean> = mIsWrongSource

    var mTagMode = false
    var mCanInsert = true
    var mLastValue = "0"
    var mCurrentX = 0
    var mCurrentY = 0
    var mCurrentBlock = 0
    private var mCurrentPassNum = 0
    private var mGameSession = UUID.randomUUID().toString()

    private val mRemainingSecondsSource = MutableLiveData(CountdownTimerContract.DEFAULT_TOTAL_SECONDS)
    val mRemainingSeconds: LiveData<Int> = mRemainingSecondsSource
    private val mTimerFinishedSource = MutableLiveData(false)
    val mTimerFinished: LiveData<Boolean> = mTimerFinishedSource

    fun initBoard(levelNum: Int) {
        viewModelScope.launch {
            mCurrentPassNum = levelNum
            mGameSession = UUID.randomUUID().toString()
            mRemainingSecondsSource.value = CountdownTimerContract.DEFAULT_TOTAL_SECONDS
            mTimerFinishedSource.value = false
            mDb.historyDao().deleteForPass(levelNum)
            val values = mDb.problemDao().getValuesForLevel(levelNum)
            if (values.size != 81 || values.any { it !in 0..9 }) return@launch
            val board = Array(9) { row ->
                Array(9) { col ->
                    val idx = row * 9 + col
                    val value = values.getOrElse(idx) { 0 }
                    CellData(
                        mRow = row, mCol = col,
                        mValue = value.toString(),
                        mBlock = (row / 3) * 3 + (col / 3) + 1,
                        mType = if (value == 0) EMPTY else PROBLEM
                    )
                }
            }
            mBoardSource.value = board
        }
    }

    fun updateRemainingSeconds(seconds: Int) {
        /*
         * 后台服务是唯一倒计时源；视图模型只保存广播传来的状态，
         * 避免页面重建后出现多个计时器同时递减。
         */
        val safeSeconds = seconds.coerceAtLeast(0)
        mRemainingSecondsSource.value = safeSeconds
        if (safeSeconds == 0 && mTimerFinishedSource.value != true) {
            mTimerFinishedSource.value = true
        }
    }

    fun getRemainingSeconds(): Int {
        return mRemainingSecondsSource.value ?: CountdownTimerContract.DEFAULT_TOTAL_SECONDS
    }

    fun setCurrentPosition(x: Int, y: Int, block: Int) {
        mCurrentX = x; mCurrentY = y; mCurrentBlock = block
    }

    fun selectCell(x: Int, y: Int) {
        val b = mBoardSource.value ?: return
        val number = b[x][y].mValue
        mLastValue = number
        b[x][y].mStatus = BE_SELECTED
        if (number == "0") tapEmpty(x, y, b[x][y].mBlock)
        else tapNoneEmpty(x, y, number)
        mBoardSource.value = b
    }

    private fun tapNoneEmpty(currentX: Int, currentY: Int, number: String) {
        val b = mBoardSource.value ?: return
        for (i in 0 until 9)
            for (j in 0 until 9)
                if (i != currentX || j != currentY)
                    b[i][j].mStatus = if (b[i][j].mValue == number) SELECT_ON else SELECT_NONE
        mBoardSource.value = b
    }

    private fun tapEmpty(currentX: Int, currentY: Int, block: Int) {
        val b = mBoardSource.value ?: return
        for (i in 0 until 9)
            for (j in 0 until 9)
                if (i != currentX || j != currentY)
                    b[i][j].mStatus = if (b[i][j].mRow == currentX || b[i][j].mCol == currentY || b[i][j].mBlock == block) SELECT_ON else SELECT_NONE
        mBoardSource.value = b
    }

    suspend fun insertNumber(x: Int, y: Int, number: String): Boolean {
        if (!isValidCell(x, y) || !isValidNumber(number) || mCurrentPassNum == 0) return false
        val b = mBoardSource.value ?: return false
        val cell = b[x][y]
        val historyDao = mDb.historyDao()

        /*
         * 再次输入同一个数字表示清空当前格子，同时记录撤销历史。
         */
        if (cell.mValue == number) {
            historyDao.insert(newHistory(x, y, TYPE_NUMBER, mLastValue.toIntOrNull() ?: 0))
            historyDao.trimToLimit(mCurrentPassNum, mGameSession)
            mLastValue = "0"; cell.mValue = "0"; cell.mStatus = BE_SELECTED
            tapEmpty(x, y, cell.mBlock)
            mBoardSource.value = b
            return false
        }

        cell.mValue = number; cell.mStatus = SELECT_ON
        var wrongCount = 0
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (i != x || j != y) {
                    if (b[i][j].mValue == number && (b[i][j].mRow == x || b[i][j].mCol == y || b[i][j].mBlock == cell.mBlock)) {
                        b[i][j].mStatus = WRONG; cell.mStatus = WRONG
                    } else if (b[i][j].mValue == number) {
                        b[i][j].mStatus = SELECT_ON
                    } else b[i][j].mStatus = SELECT_NONE
                }
                if (b[i][j].mValue == "0" || b[i][j].mStatus == WRONG) wrongCount++
            }
        }
        mBoardSource.value = b

        if (cell.mStatus == WRONG) { mIsWrongSource.value = true; return false }

        historyDao.insert(newHistory(x, y, TYPE_NUMBER, mLastValue.toIntOrNull() ?: 0))
        historyDao.trimToLimit(mCurrentPassNum, mGameSession)
        mLastValue = cell.mValue

        if (wrongCount == 0) mHasWonSource.value = true
        return true
    }

    fun revertWrongInput(x: Int, y: Int) {
        if (!isValidCell(x, y)) return
        val b = mBoardSource.value ?: return
        b[x][y].mValue = mLastValue; b[x][y].mStatus = BE_SELECTED
        if (mLastValue == "0") tapEmpty(x, y, b[x][y].mBlock)
        else tapNoneEmpty(x, y, mLastValue)
        mIsWrongSource.value = false
        mBoardSource.value = b
    }

    suspend fun insertOrRemoveTag(x: Int, y: Int, number: String, tagData: Array<Array<TagData?>>): Boolean {
        if (!isValidCell(x, y) || !isValidNumber(number) || mCurrentPassNum == 0) return false
        val td = tagData[x][y] ?: return false
        mDb.historyDao().insert(newHistory(x, y, TYPE_TAG, number.toIntOrNull() ?: 0))
        mDb.historyDao().trimToLimit(mCurrentPassNum, mGameSession)
        return if (!td.haveTag(number)) { td.setTag(number); true }
        else { td.deleteTag(number); false }
    }

    suspend fun undo(): HistoryEntity? {
        if (mCurrentPassNum == 0) return null
        val h = mDb.historyDao().getLatest(mCurrentPassNum, mGameSession) ?: return null
        mDb.historyDao().deleteById(h.mId)
        return h
    }

    suspend fun clearHistory() {
        if (mCurrentPassNum != 0) mDb.historyDao().deleteForSession(mCurrentPassNum, mGameSession)
    }

    suspend fun updatePassStatus(
        username: String,
        passNum: Int,
        nextPassNum: Int
    ) {
        val safeUsername = ensureUserMap(username)
        val userMapDao = mDb.userMapDao()
        userMapDao.updateStatus(safeUsername, passNum, "已通关")
        val map = userMapDao.getByUserAndPass(safeUsername, passNum)
        val newTimes = ((map?.mPlayTime?.toIntOrNull() ?: 0) + 1).toString()
        userMapDao.updatePlayTime(safeUsername, passNum, newTimes)
        if (passNum < 40) userMapDao.updateStatus(safeUsername, nextPassNum, "待通关")
    }

    suspend fun updatePassStatus(passNum: Int, nextPassNum: Int) {
        updatePassStatus(LauncherSessionReader.GUEST_USERNAME, passNum, nextPassNum)
    }

    fun clearWinState() { mHasWonSource.value = false }

    private fun isValidCell(x: Int, y: Int) = x in 0..8 && y in 0..8

    private fun isValidNumber(number: String) = number.toIntOrNull() in 1..9

    private suspend fun ensureUserMap(username: String): String {
        val safeUsername = username.trim().ifEmpty { LauncherSessionReader.GUEST_USERNAME }
        if (mDb.userMapDao().getAllForUser(safeUsername).isNotEmpty()) return safeUsername

        val userRows = mDb.mapDao().getAllMaps().mapIndexed { index, map ->
            UserMapEntity(
                mUsername = safeUsername,
                mPassNum = map.mPassNum,
                mStatus = if (index == 0) "待通关" else "未通关",
                mPlayTime = "0"
            )
        }
        mDb.userMapDao().insertAll(userRows)
        return safeUsername
    }

    private fun newHistory(row: Int, col: Int, type: Int, value: Int): HistoryEntity {
        return HistoryEntity(
            mRow = row,
            mCol = col,
            mType = type,
            mValue = value,
            mPassNum = mCurrentPassNum,
            mGameSession = mGameSession
        )
    }
}
