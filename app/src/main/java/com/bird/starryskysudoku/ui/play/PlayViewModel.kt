package com.bird.starryskysudoku.ui.play

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.HistoryEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayViewModel(private val db: AppDatabase) : ViewModel() {

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
        var row: Int,
        var col: Int,
        var value: String,
        var block: Int,
        var status: Int = SELECT_NONE,
        var type: Int = EMPTY
    )

    private val _board = MutableLiveData<Array<Array<CellData>>>()
    val board: LiveData<Array<Array<CellData>>> = _board

    private val _hasWon = MutableLiveData(false)
    val hasWon: LiveData<Boolean> = _hasWon

    private val _isWrong = MutableLiveData(false)
    val isWrong: LiveData<Boolean> = _isWrong

    var tagMode = false
    var canInsert = true
    var lastValue = "0"
    var currentX = 0
    var currentY = 0
    var currentBlock = 0

    private var timerJob: Job? = null
    private val _remainingSeconds = MutableLiveData(600) // 10min
    val remainingSeconds: LiveData<Int> = _remainingSeconds
    private val _timerFinished = MutableLiveData(false)
    val timerFinished: LiveData<Boolean> = _timerFinished

    fun initBoard(levelNum: Int) {
        viewModelScope.launch {
            val values = db.problemDao().getValuesForLevel(levelNum)
            val board = Array(9) { row ->
                Array(9) { col ->
                    val idx = row * 9 + col
                    val value = values.getOrElse(idx) { 0 }
                    CellData(
                        row = row, col = col,
                        value = value.toString(),
                        block = (row / 3) * 3 + (col / 3) + 1,
                        type = if (value == 0) EMPTY else PROBLEM
                    )
                }
            }
            _board.value = board
        }
    }

    fun startTimer(onTick: ((Int) -> Unit)? = null) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value!! > 0) {
                delay(1000)
                val current = _remainingSeconds.value!! - 1
                _remainingSeconds.value = current
                onTick?.invoke(current)
            }
            _timerFinished.value = true
        }
    }

    fun pauseTimer() { timerJob?.cancel() }

    fun setCurrentPosition(x: Int, y: Int, block: Int) {
        currentX = x; currentY = y; currentBlock = block
    }

    fun selectCell(x: Int, y: Int) {
        val b = _board.value ?: return
        val number = b[x][y].value
        lastValue = number
        b[x][y].status = BE_SELECTED
        if (number == "0") tapEmpty(x, y, b[x][y].block)
        else tapNoneEmpty(x, y, number)
        _board.value = b
    }

    private fun tapNoneEmpty(currentX: Int, currentY: Int, number: String) {
        val b = _board.value ?: return
        for (i in 0 until 9)
            for (j in 0 until 9)
                if (i != currentX || j != currentY)
                    b[i][j].status = if (b[i][j].value == number) SELECT_ON else SELECT_NONE
        _board.value = b
    }

    private fun tapEmpty(currentX: Int, currentY: Int, block: Int) {
        val b = _board.value ?: return
        for (i in 0 until 9)
            for (j in 0 until 9)
                if (i != currentX || j != currentY)
                    b[i][j].status = if (b[i][j].row == currentX || b[i][j].col == currentY || b[i][j].block == block) SELECT_ON else SELECT_NONE
        _board.value = b
    }

    suspend fun insertNumber(x: Int, y: Int, number: String): Boolean {
        val b = _board.value ?: return false
        val cell = b[x][y]
        val historyDao = db.historyDao()

        // Insert same number = clear
        if (cell.value == number) {
            historyDao.insert(HistoryEntity(row = x, col = y, type = TYPE_NUMBER, value = lastValue.toIntOrNull() ?: 0))
            historyDao.trimToLimit()
            lastValue = "0"; cell.value = "0"; cell.status = BE_SELECTED
            tapEmpty(x, y, cell.block)
            _board.value = b
            return false
        }

        cell.value = number; cell.status = SELECT_ON
        var wrongCount = 0
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                if (i != x || j != y) {
                    if (b[i][j].value == number && (b[i][j].row == x || b[i][j].col == y || b[i][j].block == cell.block)) {
                        b[i][j].status = WRONG; cell.status = WRONG
                    } else if (b[i][j].value == number) {
                        b[i][j].status = SELECT_ON
                    } else b[i][j].status = SELECT_NONE
                }
                if (b[i][j].value == "0" || b[i][j].status == WRONG) wrongCount++
            }
        }
        _board.value = b

        if (cell.status == WRONG) { _isWrong.value = true; return false }

        historyDao.insert(HistoryEntity(row = x, col = y, type = TYPE_NUMBER, value = lastValue.toIntOrNull() ?: 0))
        historyDao.trimToLimit()
        lastValue = cell.value

        if (wrongCount == 0) _hasWon.value = true
        return true
    }

    fun revertWrongInput(x: Int, y: Int) {
        val b = _board.value ?: return
        b[x][y].value = lastValue; b[x][y].status = BE_SELECTED
        if (lastValue == "0") tapEmpty(x, y, b[x][y].block)
        else tapNoneEmpty(x, y, lastValue)
        _isWrong.value = false
        _board.value = b
    }

    suspend fun insertOrRemoveTag(x: Int, y: Int, number: String, tagData: Array<Array<TagData?>>): Boolean {
        val td = tagData[x][y] ?: return false
        db.historyDao().insert(HistoryEntity(row = x, col = y, type = TYPE_TAG, value = number.toIntOrNull() ?: 0))
        db.historyDao().trimToLimit()
        return if (!td.haveTag(number)) { td.setTag(number); true }
        else { td.deleteTag(number); false }
    }

    suspend fun undo(): HistoryEntity? {
        val h = db.historyDao().getLatest() ?: return null
        db.historyDao().deleteById(h.id)
        return h
    }

    suspend fun clearHistory() { db.historyDao().deleteAll() }

    suspend fun updatePassStatus(passNum: Int, nextPassNum: Int) {
        db.mapDao().updateStatus(passNum, "已通关")
        val map = db.mapDao().getMapByNum(passNum)
        val newTimes = ((map?.playTime?.toIntOrNull() ?: 0) + 1).toString()
        db.mapDao().updatePlayTime(passNum, newTimes)
        if (passNum < 40) db.mapDao().updateStatus(nextPassNum, "待通关")
    }

    fun clearWinState() { _hasWon.value = false }
}
