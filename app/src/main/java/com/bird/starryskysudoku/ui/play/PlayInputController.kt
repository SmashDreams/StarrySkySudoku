package com.bird.starryskysudoku.ui.play

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic
import kotlinx.coroutines.launch

class PlayInputController(
    private val mScope: LifecycleCoroutineScope,
    private val mViewModel: PlayViewModel,
    private val mBroadView: BroadView,
    private val mNumbers: Array<TextView?>,
    private val mRevoke: ImageView,
    private val mTag: ImageView,
    private val mTagData: Array<Array<TagData?>>,
    private val mGetLevel: () -> Int,
    private val mGetUsername: () -> String,
    private val mOnPuzzleCompleted: suspend (Int) -> Unit
) {
    fun init() {
        initTagButton()
        initInsertButtons()
        initRevokeButton()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTagButton() {
        mTag.setOnTouchListener { view, event ->
            if (!canAnimateSelectedCell()) return@setOnTouchListener false
            animatePress(view as ImageView, event)
            false
        }

        mTag.setOnClickListener {
            if (mViewModel.mHasWon.value == true) return@setOnClickListener
            val cell = currentCell() ?: return@setOnClickListener
            if (cell.mType == PlayViewModel.PROBLEM) {
                PlayMusic.getInstance().playInputWrong()
                return@setOnClickListener
            }
            if (mViewModel.mCurrentBlock == 0 || cell.mValue != "0") {
                PlayMusic.getInstance().playInputWrong()
                return@setOnClickListener
            }
            PlayMusic.getInstance().playButtonTap()
            if (!mViewModel.mTagMode) {
                mTag.setImageResource(R.drawable.icon_notes_on)
                mViewModel.mTagMode = true
                refreshNumberAlphaForTags()
            } else {
                for (button in mNumbers) button?.alpha = 1f
                mTag.setImageResource(R.drawable.icon_notes_off)
                mViewModel.mTagMode = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initInsertButtons() {
        for (index in 0 until 9) {
            val numberIndex = index
            mNumbers[index]?.setOnTouchListener { view, event ->
                if (!canAnimateSelectedCell()) return@setOnTouchListener false
                animatePress(view as TextView, event)
                false
            }

            mNumbers[index]?.setOnClickListener {
                if (mViewModel.mHasWon.value == true || !mViewModel.mCanInsert) return@setOnClickListener
                val cell = currentCell() ?: return@setOnClickListener
                if (cell.mType == PlayViewModel.PROBLEM || mViewModel.mCurrentBlock == 0) {
                    PlayMusic.getInstance().playInputWrong()
                    return@setOnClickListener
                }
                val number = (numberIndex + 1).toString()
                if (!mViewModel.mTagMode) {
                    insertNumber(number)
                } else {
                    insertOrRemoveTag(cell, number, numberIndex)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initRevokeButton() {
        mRevoke.alpha = 0.55f
        mRevoke.setOnTouchListener { view, event ->
            if (!canAnimateSelectedCell()) return@setOnTouchListener false
            animatePress(view as ImageView, event)
            false
        }

        mRevoke.setOnClickListener {
            if (mViewModel.mHasWon.value == true || mViewModel.mCurrentBlock == 0) return@setOnClickListener
            mScope.launch {
                val history = mViewModel.undo()
                if (history == null) {
                    clearSelectionAfterEmptyUndo()
                    return@launch
                }
                restoreHistory(history)
                mBroadView.invalidate()
            }
        }
    }

    private fun insertNumber(number: String) {
        mScope.launch {
            mViewModel.insertNumber(mViewModel.mCurrentX, mViewModel.mCurrentY, number)
            mRevoke.alpha = 1f
            mTag.alpha = 0.55f
            mBroadView.initData(mViewModel.mBoard.value!!)
            mBroadView.invalidate()

            if (mViewModel.mHasWon.value == true) {
                val level = mGetLevel()
                mViewModel.updatePassStatus(mGetUsername(), level, level + 1)
                mOnPuzzleCompleted(level)
            }
        }
    }

    private fun insertOrRemoveTag(
        cell: PlayViewModel.CellData,
        number: String,
        numberIndex: Int
    ) {
        if (cell.mValue != "0") {
            PlayMusic.getInstance().playInputWrong()
            return
        }
        PlayMusic.getInstance().playButtonTap()
        mRevoke.alpha = 1f
        mScope.launch {
            val added = mViewModel.insertOrRemoveTag(
                mViewModel.mCurrentX,
                mViewModel.mCurrentY,
                number,
                mTagData
            )
            mNumbers[numberIndex]?.alpha = if (added) 0.55f else 1f
            mBroadView.initTagData(mTagData)
            mBroadView.invalidate()
        }
    }

    private fun clearSelectionAfterEmptyUndo() {
        PlayMusic.getInstance().playInputWrong()
        mRevoke.alpha = 0.55f
        mTag.alpha = 1f
        mTag.setImageResource(R.drawable.icon_notes_off)
        mViewModel.mTagMode = false
        mViewModel.mCurrentBlock = 0
        val board = mViewModel.mBoard.value!!
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                board[row][col].mStatus = PlayViewModel.SELECT_NONE
            }
        }
        mBroadView.initData(board)
        mBroadView.invalidate()
    }

    private fun restoreHistory(history: com.bird.starryskysudoku.data.entity.HistoryEntity) {
        mViewModel.mCurrentX = history.mRow
        mViewModel.mCurrentY = history.mCol
        mViewModel.mCurrentBlock = mViewModel.mBoard.value
            ?.get(history.mRow)
            ?.get(history.mCol)
            ?.mBlock ?: 0
        PlayMusic.getInstance().playButtonTap()

        if (history.mType == PlayViewModel.TYPE_NUMBER) {
            restoreNumberHistory(history)
        } else {
            restoreTagHistory(history)
        }
    }

    private fun restoreNumberHistory(history: com.bird.starryskysudoku.data.entity.HistoryEntity) {
        mViewModel.mTagMode = false
        mViewModel.mLastValue = history.mValue.toString()
        mTag.setImageResource(R.drawable.icon_notes_off)
        for (button in mNumbers) button?.alpha = 1f

        val board = mViewModel.mBoard.value!!
        board[history.mRow][history.mCol].mValue = history.mValue.toString()
        board[history.mRow][history.mCol].mStatus = PlayViewModel.BE_SELECTED
        mViewModel.selectCell(history.mRow, history.mCol)
        mTag.alpha = if (history.mValue == 0) 1f else 0.55f
        mBroadView.initData(board)
    }

    private fun restoreTagHistory(history: com.bird.starryskysudoku.data.entity.HistoryEntity) {
        val board = mViewModel.mBoard.value!!
        board[history.mRow][history.mCol].mStatus = PlayViewModel.BE_SELECTED
        mTag.alpha = 1f
        mViewModel.mTagMode = true
        mTag.setImageResource(R.drawable.icon_notes_on)

        val tagData = mTagData[history.mRow][history.mCol]!!
        val historyValue = history.mValue.toString()
        if (tagData.haveTag(historyValue)) {
            tagData.deleteTag(historyValue)
            mNumbers[history.mValue - 1]?.alpha = 1f
        } else {
            tagData.setTag(historyValue)
            mNumbers[history.mValue - 1]?.alpha = 0.55f
        }
        mBroadView.initTagData(mTagData)

        for (index in 0 until 9) {
            mNumbers[index]?.alpha = if (tagData.haveTag((index + 1).toString())) 0.55f else 1f
        }
        mBroadView.initData(board)
    }

    private fun refreshNumberAlphaForTags() {
        val tagData = mTagData[mViewModel.mCurrentX][mViewModel.mCurrentY]
        for (index in 0 until 9) {
            mNumbers[index]?.alpha = if (tagData != null && tagData.haveTag((index + 1).toString())) {
                0.55f
            } else {
                1f
            }
        }
    }

    private fun currentCell(): PlayViewModel.CellData? {
        return mViewModel.mBoard.value?.get(mViewModel.mCurrentX)?.get(mViewModel.mCurrentY)
    }

    private fun canAnimateSelectedCell(): Boolean {
        val cell = currentCell()
        return mViewModel.mCurrentBlock != 0 &&
            cell?.mType != PlayViewModel.PROBLEM &&
            mViewModel.mHasWon.value != true
    }

    private fun animatePress(view: android.view.View, event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }
    }
}
