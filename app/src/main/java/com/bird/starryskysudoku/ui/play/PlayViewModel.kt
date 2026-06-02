package com.bird.starryskysudoku.ui.play

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.entity.HistoryEntity
import com.bird.starryskysudoku.data.repository.PlayRepository
import com.bird.starryskysudoku.timer.CountdownTimerContract
import kotlinx.coroutines.launch
import java.util.UUID

class PlayViewModel(private val mPlayRepository: PlayRepository) : ViewModel() {

    companion object {
        const val SELECT_NONE = BoardCell.SELECT_NONE
        const val SELECT_ON = BoardCell.SELECT_ON
        const val BE_SELECTED = BoardCell.BE_SELECTED
        const val WRONG = BoardCell.WRONG
        const val PROBLEM = BoardCell.PROBLEM
        const val EMPTY = BoardCell.EMPTY
        const val TYPE_NUMBER = 0
        const val TYPE_TAG = 1
        private const val BOARD_SIZE = 9
    }

    data class RestoredHistoryState(
        val mBoard: Array<Array<BoardCell>>,
        val mTagEnabled: Boolean,
        val mTagAlpha: Float,
        val mNumberAlphas: FloatArray
    )

    private val mBoardSource = MutableLiveData<Array<Array<BoardCell>>>()
    val mBoard: LiveData<Array<Array<BoardCell>>> = mBoardSource

    private val mHasWonSource = MutableLiveData(false)
    val mHasWon: LiveData<Boolean> = mHasWonSource

    private val mIsWrongSource = MutableLiveData(false)
    val mIsWrong: LiveData<Boolean> = mIsWrongSource

    private var mTagMode = false
    private var mCanInsert = true
    private var mLastValue = "0"
    // 当前选中格子的行列与宫号统一保存在这里，供输入、撤销和高亮复用。
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
    // 同一局只允许记录一次通关或失败结果，避免多个回调重复入库。
    private val mGameResultRecordGate = GameResultRecordGate()

    private val mRemainingSecondsSource = MutableLiveData(CountdownTimerContract.DEFAULT_TOTAL_SECONDS)
    val mRemainingSeconds: LiveData<Int> = mRemainingSecondsSource
    private val mTimerFinishedSource = MutableLiveData(false)
    val mTimerFinished: LiveData<Boolean> = mTimerFinishedSource

    fun initBoard(levelNum: Int) {
        viewModelScope.launch {
            mCurrentPassNum = levelNum
            // 每次进入新棋盘都生成新的对局标识，用来隔离撤销历史。
            mGameSession = UUID.randomUUID().toString()
            mRemainingSecondsSource.value = CountdownTimerContract.DEFAULT_TOTAL_SECONDS
            mTimerFinishedSource.value = false
            mBoardSource.value = mPlayRepository.loadBoard(levelNum) ?: return@launch
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

    fun currentCell(): BoardCell? {
        return mBoardSource.value?.get(getCurrentRow())?.get(getCurrentCol())
    }

    fun selectCell(row: Int, col: Int) {
        val board = mBoardSource.value ?: return
        mLastValue = PlayBoardRules.selectCell(board, row, col)
        mBoardSource.value = board
    }

    suspend fun insertNumber(x: Int, y: Int, number: String): Boolean {
        if (!isValidCell(x, y) || !isValidNumber(number) || mCurrentPassNum == 0) return false
        val board = mBoardSource.value ?: return false
        val previousValue = mLastValue
        val result = PlayBoardRules.insertNumber(board, x, y, number, previousValue)
        mBoardSource.value = board

        if (result.mWasCleared) {
            // 清空格子也要进历史，保证撤销后能回到原数字。
            recordHistory(x, y, TYPE_NUMBER, mLastValue.toIntOrNull() ?: 0)
            mLastValue = result.mNewLastValue
            return false
        }

        if (result.mIsWrong) { mIsWrongSource.value = true; return false }

        recordHistory(x, y, TYPE_NUMBER, previousValue.toIntOrNull() ?: 0)
        mLastValue = result.mNewLastValue

        if (result.mIsComplete) mHasWonSource.value = true
        return true
    }

    fun revertWrongInput(x: Int, y: Int) {
        if (!isValidCell(x, y)) return
        val board = mBoardSource.value ?: return
        PlayBoardRules.revertWrongInput(board, x, y, mLastValue)
        mIsWrongSource.value = false
        mBoardSource.value = board
    }

    suspend fun insertOrRemoveTag(x: Int, y: Int, number: String, tagData: Array<Array<TagData?>>): Boolean {
        if (!isValidCell(x, y) || !isValidNumber(number) || mCurrentPassNum == 0) return false
        val td = tagData[x][y] ?: return false
        // 候选数的增删也走统一历史栈，这样撤销行为对数字和笔记一致。
        recordHistory(x, y, TYPE_TAG, number.toIntOrNull() ?: 0)
        return if (!td.haveTag(number)) { td.setTag(number); true }
        else { td.deleteTag(number); false }
    }

    suspend fun undo(): HistoryEntity? {
        if (mCurrentPassNum == 0) return null
        return mPlayRepository.undo(mCurrentPassNum, mGameSession)
    }

    fun clearSelectionAfterEmptyUndo(): Array<Array<BoardCell>>? {
        mTagMode = false
        setCurrentPosition(getCurrentRow(), getCurrentCol(), 0)
        val board = mBoardSource.value ?: return null
        // 空撤销后没有明确焦点，统一清空整盘高亮，避免界面残留旧选中态。
        for (row in 0 until BOARD_SIZE) {
            for (col in 0 until BOARD_SIZE) {
                board[row][col].mStatus = SELECT_NONE
            }
        }
        mBoardSource.value = board
        return board
    }

    fun restoreHistory(
        history: HistoryEntity,
        tagData: Array<Array<TagData?>>
    ): RestoredHistoryState? {
        val board = mBoardSource.value ?: return null
        val block = board.getOrNull(history.mRow)?.getOrNull(history.mCol)?.mBlock ?: 0
        setCurrentPosition(history.mRow, history.mCol, block)

        return if (history.mType == TYPE_NUMBER) {
            restoreNumberHistory(board, history)
        } else {
            restoreTagHistory(board, history, tagData)
        }
    }

    suspend fun clearHistory() {
        if (mCurrentPassNum != 0) mPlayRepository.clearHistory(mCurrentPassNum, mGameSession)
    }

    suspend fun updatePassStatus(
        username: String,
        passNum: Int,
        nextPassNum: Int
    ) {
        mPlayRepository.completePass(username, passNum, nextPassNum)
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

    private suspend fun recordHistory(row: Int, col: Int, type: Int, value: Int) {
        mPlayRepository.recordHistory(newHistory(row, col, type, value), mCurrentPassNum, mGameSession)
    }

    private fun restoreNumberHistory(
        board: Array<Array<BoardCell>>,
        history: HistoryEntity
    ): RestoredHistoryState {
        mTagMode = false
        mLastValue = history.mValue.toString()
        // 数字撤销恢复后，重新走一次选中逻辑，确保关联高亮和最近值一致。
        board[history.mRow][history.mCol].mValue = history.mValue.toString()
        board[history.mRow][history.mCol].mStatus = BE_SELECTED
        selectCell(history.mRow, history.mCol)
        val tagAlpha = if (history.mValue == 0) 1f else 0.55f
        return RestoredHistoryState(
            mBoard = requireNotNull(mBoardSource.value),
            mTagEnabled = false,
            mTagAlpha = tagAlpha,
            mNumberAlphas = FloatArray(BOARD_SIZE) { 1f }
        )
    }

    private fun restoreTagHistory(
        board: Array<Array<BoardCell>>,
        history: HistoryEntity,
        tagData: Array<Array<TagData?>>
    ): RestoredHistoryState {
        board[history.mRow][history.mCol].mStatus = BE_SELECTED
        mTagMode = true
        val cellTags = tagData[history.mRow][history.mCol] ?: TagData().also {
            tagData[history.mRow][history.mCol] = it
        }
        val historyValue = history.mValue.toString()
        // 笔记历史本质上是一次“切换”操作，恢复时再执行一次同样的切换即可。
        if (cellTags.haveTag(historyValue)) {
            cellTags.deleteTag(historyValue)
        } else {
            cellTags.setTag(historyValue)
        }
        mBoardSource.value = board
        return RestoredHistoryState(
            mBoard = board,
            mTagEnabled = true,
            mTagAlpha = 1f,
            mNumberAlphas = FloatArray(BOARD_SIZE) { index ->
                if (cellTags.haveTag((index + 1).toString())) 0.55f else 1f
            }
        )
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
