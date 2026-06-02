package com.bird.starryskysudoku.ui.play

import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.bird.starryskysudoku.media.PlayMusic

class PlayBoardController(
    private val mLifecycleOwner: LifecycleOwner,
    private val mViewModel: PlayViewModel,
    private val mBroadView: BroadView,
    private val mNumbers: Array<TextView?>,
    private val mTag: ImageView,
    private val mRevoke: ImageView,
    private val mTagData: Array<Array<TagData?>>,
    private val mGetLevel: () -> Int
) {
    fun init() {
        // 先初始化棋盘数据，再建立观察和触摸回调，避免首次渲染拿到空引用。
        disableCellActions()
        mViewModel.initBoard(mGetLevel())
        observeBoard()
        initTouchListener()
    }

    private fun observeBoard() {
        mViewModel.mBoard.observe(mLifecycleOwner) { board ->
            ensureTagData(board)
            mBroadView.initData(board)
            mBroadView.initTagData(mTagData)
            mBroadView.invalidate()
        }
    }

    private fun ensureTagData(board: Array<Array<BoardCell>>) {
        // 只有空格才需要候选数容器，题面数字不浪费额外对象。
        for (row in 0 until 9) {
            for (col in 0 until 9) {
                if (board[row][col].mValue == "0" && mTagData[row][col] == null) {
                    mTagData[row][col] = TagData()
                }
            }
        }
    }

    private fun initTouchListener() {
        mBroadView.setListener(object : BroadView.Listener {
            override fun onTouch(row: Int, col: Int, block: Int) {
                mViewModel.setCurrentPosition(row, col, block)
                mViewModel.selectCell(row, col)

                val cell = mViewModel.mBoard.value?.get(row)?.get(col) ?: return
                // 每次选格后都同步更新数字键和笔记按钮的可点击视觉状态。
                refreshCellActionAlpha(cell, row, col)
                mBroadView.invalidate()
                PlayMusic.getInstance().playButtonTap()
            }
        })
    }

    private fun refreshCellActionAlpha(
        cell: BoardCell,
        row: Int,
        col: Int
    ) {
        when {
            cell.mType == PlayViewModel.PROBLEM -> {
                setNumberEnabled(false)
                setActionEnabled(mTag, false)
            }
            cell.mValue != "0" -> {
                setNumberEnabled(true)
                setActionEnabled(mTag, false)
            }
            else -> {
                setActionEnabled(mTag, true)
                if (mViewModel.isTagMode()) {
                    refreshTagNumberAlpha(row, col)
                } else {
                    setNumberEnabled(true)
                }
            }
        }
        setActionEnabled(mRevoke, true)
    }

    private fun refreshTagNumberAlpha(row: Int, col: Int) {
        val tagData = mTagData[row][col]
        for (index in 0 until 9) {
            val tagged = tagData != null && tagData.haveTag((index + 1).toString())
            mNumbers[index]?.isEnabled = true
            mNumbers[index]?.alpha = if (tagged) 0.55f else 1f
        }
    }

    private fun disableCellActions() {
        setNumberEnabled(false)
        setActionEnabled(mTag, false)
        setActionEnabled(mRevoke, false)
    }

    private fun setNumberEnabled(enabled: Boolean) {
        for (number in mNumbers) {
            number?.isEnabled = enabled
            number?.alpha = if (enabled) 1f else 0.55f
        }
    }

    private fun setActionEnabled(view: ImageView, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.55f
    }
}
