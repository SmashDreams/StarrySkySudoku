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
        // 输入控制拆成标签、数字、撤销三组事件，便于分别维护交互规则。
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
            // 题面数字和已填数字都不允许进入笔记模式。
            if (cell.mType == PlayViewModel.PROBLEM) {
                PlayMusic.getInstance().playInputWrong()
                return@setOnClickListener
            }
            if (mViewModel.getCurrentBlock() == 0 || cell.mValue != "0") {
                PlayMusic.getInstance().playInputWrong()
                return@setOnClickListener
            }
            PlayMusic.getInstance().playButtonTap()
            if (!mViewModel.isTagMode()) {
                mTag.setImageResource(R.drawable.icon_notes_on)
                mViewModel.setTagMode(true)
                refreshNumberAlphaForTags()
            } else {
                for (button in mNumbers) button?.alpha = 1f
                mTag.setImageResource(R.drawable.icon_notes_off)
                mViewModel.setTagMode(false)
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
                if (mViewModel.mHasWon.value == true || !mViewModel.canInsert()) return@setOnClickListener
                val cell = currentCell() ?: return@setOnClickListener
                // 没有选中有效空格时，数字键一律按错误输入处理。
                if (cell.mType == PlayViewModel.PROBLEM || mViewModel.getCurrentBlock() == 0) {
                    PlayMusic.getInstance().playInputWrong()
                    return@setOnClickListener
                }
                val number = (numberIndex + 1).toString()
                if (!mViewModel.isTagMode()) {
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
            if (mViewModel.mHasWon.value == true || mViewModel.getCurrentBlock() == 0) return@setOnClickListener
            mScope.launch {
                // 撤销既可能恢复数字，也可能恢复笔记，所以统一从历史栈取最近一步。
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
            mViewModel.insertNumber(mViewModel.getCurrentRow(), mViewModel.getCurrentCol(), number)
            mRevoke.alpha = 1f
            mTag.alpha = 0.55f
            mViewModel.mBoard.value?.let { mBroadView.initData(it) }
            mBroadView.invalidate()

            if (mViewModel.mHasWon.value == true) {
                val level = mGetLevel()
                // 通关后先更新地图进度，再异步记录战绩。
                mViewModel.updatePassStatus(mGetUsername(), level, level + 1)
                mOnPuzzleCompleted(level)
            }
        }
    }

    private fun insertOrRemoveTag(
        cell: BoardCell,
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
            // 候选数的明暗和笔记数据保持一一对应，点击后立即刷新当前键位透明度。
            val added = mViewModel.insertOrRemoveTag(
                mViewModel.getCurrentRow(),
                mViewModel.getCurrentCol(),
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
        // 没有历史可撤销时，回到无选中基线状态并提示用户。
        mViewModel.clearSelectionAfterEmptyUndo()?.let { board ->
            mBroadView.initData(board)
        }
        mBroadView.invalidate()
    }

    private fun restoreHistory(history: com.bird.starryskysudoku.data.entity.HistoryEntity) {
        PlayMusic.getInstance().playButtonTap()
        // 视图模型负责还原数据，本控制器只把按钮透明度和棋盘视图同步回对应状态。
        val restored = mViewModel.restoreHistory(history, mTagData) ?: return
        mTag.alpha = restored.mTagAlpha
        mTag.setImageResource(if (restored.mTagEnabled) R.drawable.icon_notes_on else R.drawable.icon_notes_off)
        for (index in 0 until 9) {
            mNumbers[index]?.alpha = restored.mNumberAlphas[index]
        }
        mBroadView.initTagData(mTagData)
        mBroadView.initData(restored.mBoard)
    }

    private fun refreshNumberAlphaForTags() {
        val tagData = mTagData[mViewModel.getCurrentRow()][mViewModel.getCurrentCol()]
        for (index in 0 until 9) {
            mNumbers[index]?.alpha = if (tagData != null && tagData.haveTag((index + 1).toString())) {
                0.55f
            } else {
                1f
            }
        }
    }

    private fun currentCell(): BoardCell? {
        return mViewModel.currentCell()
    }

    private fun canAnimateSelectedCell(): Boolean {
        val cell = currentCell()
        return mViewModel.getCurrentBlock() != 0 &&
            cell?.mType != PlayViewModel.PROBLEM &&
            mViewModel.mHasWon.value != true
    }

    private fun animatePress(view: android.view.View, event: MotionEvent) {
        // 这里统一按压缩放反馈，避免按钮和数字键各自维护一套动画。
        when (event.action) {
            MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }
    }
}
