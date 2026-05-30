package com.bird.starryskysudoku.ui.play

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.HistoryEntity
import com.bird.starryskysudoku.data.repository.UserProgressRepository
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

    private var mTagMode = false
    private var mCanInsert = true
    private var mLastValue = "0"
    private var mCurrentPosition = CellPosition(0, 0, 0)

    fun isTagMode(): Boolean = mTagMode
    fun setTagMode(enabled: Boolean) { mTagMode = enabled }
    fun canInsert(): Boolean = mCanInsert
    fun setCanInsert(enabled: Boolean) { mCanInsert = enabled }
    fun getLastValue(): String = mLastValue
    fun setLastValue(value: String) { mLastValue = value }
    fun getCurrentRow(): Int = mCurrentPosition.mRow
    fun getCurrentCol(): Int = mCurrentPosition.mCol
    fun getCurrentBlock(): Int = mCurrentPosition.mBlock
    fun setCurrentPosition(row: Int, col: Int, block: Int) {
        mCurrentPosition = CellPosition(row, col, block)
    }
    private var mCurrentPassNum = 0
    private var mGameSession = UUID.randomUUID().toString()
    private val mUserProgressRepository = UserProgressRepository(mDb)
    private val mGameResultRecordGate = GameResultRecordGate()

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
            val board = PlayBoardRules.createBoard(values) ?: return@launch
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

    fun selectCell(row: Int, col: Int) {
        val b = mBoardSource.value ?: return
        mLastValue = PlayBoardRules.selectCell(b, row, col)
        mBoardSource.value = b
    }

    suspend fun insertNumber(x: Int, y: Int, number: String): Boolean {
        if (!isValidCell(x, y) || !isValidNumber(number) || mCurrentPassNum == 0) return false
        val b = mBoardSource.value ?: return false
        val historyDao = mDb.historyDao()

        val previousValue = mLastValue
        val result = PlayBoardRules.insertNumber(b, x, y, number, previousValue)
        mBoardSource.value = b

        if (result.mWasCleared) {
            historyDao.insert(newHistory(x, y, TYPE_NUMBER, mLastValue.toIntOrNull() ?: 0))
            historyDao.trimToLimit(mCurrentPassNum, mGameSession)
            mLastValue = result.mNewLastValue
            return false
        }

        if (result.mIsWrong) { mIsWrongSource.value = true; return false }

        historyDao.insert(newHistory(x, y, TYPE_NUMBER, previousValue.toIntOrNull() ?: 0))
        historyDao.trimToLimit(mCurrentPassNum, mGameSession)
        mLastValue = result.mNewLastValue

        if (result.mIsComplete) mHasWonSource.value = true
        return true
    }

    fun revertWrongInput(x: Int, y: Int) {
        if (!isValidCell(x, y)) return
        val b = mBoardSource.value ?: return
        PlayBoardRules.revertWrongInput(b, x, y, mLastValue)
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
        mUserProgressRepository.completePass(username, passNum, nextPassNum)
    }

    suspend fun updatePassStatus(passNum: Int, nextPassNum: Int) {
        updatePassStatus(LauncherSessionReader.GUEST_USERNAME, passNum, nextPassNum)
    }

    fun clearWinState() { mHasWonSource.value = false }

    fun markGameResultRecordStarted(levelNum: Int, completed: Boolean): Boolean {
        return mGameResultRecordGate.markIfFirst(levelNum, completed)
    }

    fun clearGameResultRecordMark(levelNum: Int, completed: Boolean) {
        mGameResultRecordGate.unmark()
    }

    private fun isValidCell(x: Int, y: Int) = x in 0..8 && y in 0..8

    private fun isValidNumber(number: String) = number.toIntOrNull() in 1..9

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
