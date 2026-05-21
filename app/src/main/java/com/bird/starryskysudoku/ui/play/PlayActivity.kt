package com.bird.starryskysudoku.ui.play

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.data.provider.GameResultContract
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.timer.CountdownTimerContract
import com.bird.starryskysudoku.timer.CountdownTimerService
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.ui.map.MapActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PlayActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PlayActivity"
        private const val MAX_LEVEL = 40
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private lateinit var mViewModel: PlayViewModel
    private lateinit var mBroadView: BroadView
    private var mCountdownReceiverRegistered = false
    private val mCountdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != CountdownTimerContract.ACTION_COUNTDOWN_TICK) return
            val remainingSeconds = intent.getIntExtra(
                CountdownTimerContract.EXTRA_REMAINING_SECONDS,
                CountdownTimerContract.DEFAULT_TOTAL_SECONDS
            )
            /*
             * 广播接收器是前台页面与后台服务之间的桥梁：
             * 这里只解析剩余时间并交给视图模型，界面仍由可观察数据统一刷新。
             */
            mViewModel.updateRemainingSeconds(remainingSeconds)
        }
    }

    /*
     * 游戏页面的主要控件引用集中在这里保存，便于统一管理倒计时、棋盘和输入按钮状态。
     */
    private lateinit var mPlayNum: TextView
    private lateinit var mTimeProgressBar: ProgressBar
    private lateinit var mRemainingMins: TextView
    private lateinit var mRemainingSecs: TextView
    private lateinit var mColon: TextView
    private val mNumbers = arrayOfNulls<TextView>(9)
    private lateinit var mRevoke: ImageView
    private lateinit var mTag: ImageView
    private lateinit var mPauseButton: ImageView

    /*
     * 页面状态只保存与界面交互强相关的数据；实际棋盘数据和倒计时状态由视图模型维护。
     */
    private var mMusicOpened = true
    private var mAudioOpened = true
    private var mIsPaused = false
    private var mResultRecorded = false
    private var mNum = "1"
    private var mTagData = Array(9) { arrayOfNulls<TagData>(9) }

    /*
     * 暂停、胜利和失败弹窗贯穿游戏流程，统一持有可以避免重复创建窗口。
     */
    private lateinit var mPauseDialog: MyDialog
    private lateinit var mWinDialog: MyDialog
    private lateinit var mLoseDialog: MyDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        mNum = parseLevel(intent.getStringExtra("mNum")).toString()
        val maxNum = MAX_LEVEL

        val db = DatabaseInitializer.getDatabase(this)
        mViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlayViewModel(db) as T
            }
        })[PlayViewModel::class.java]

        val musicPrefs = getSharedPreferences("music_set", MODE_PRIVATE)
        mMusicOpened = musicPrefs.getBoolean("music", true)
        mAudioOpened = musicPrefs.getBoolean("audio", true)

        /*
         * 进入页面后一次性绑定控件，后续只更新控件内容和可用状态。
         */
        mPlayNum = findViewById(R.id.play_num); mPlayNum.text = mNum
        mTimeProgressBar = findViewById(R.id.play_time_progressbar)
        mRemainingMins = findViewById(R.id.play_time_min)
        mRemainingSecs = findViewById(R.id.play_time_sec)
        mColon = findViewById(R.id.textview74)
        mBroadView = findViewById(R.id.play_broad)
        mNumbers[0] = findViewById(R.id.play_1); mNumbers[1] = findViewById(R.id.play_2)
        mNumbers[2] = findViewById(R.id.play_3); mNumbers[3] = findViewById(R.id.play_4)
        mNumbers[4] = findViewById(R.id.play_5); mNumbers[5] = findViewById(R.id.play_6)
        mNumbers[6] = findViewById(R.id.play_7); mNumbers[7] = findViewById(R.id.play_8)
        mNumbers[8] = findViewById(R.id.play_9)
        mRevoke = findViewById(R.id.play_revoke); mTag = findViewById(R.id.play_tag)
        mPauseButton = findViewById(R.id.play_pause)

        mTimeProgressBar.max = CountdownTimerContract.DEFAULT_TOTAL_SECONDS

        initBoard()
        initTimer()
        initDialogs(maxNum)
        initTagButton()
        initInsertButtons()
        initRevokeButton()
        initPauseButton()
        initBackHandler()
    }

    private fun initBoard() {
        mViewModel.initBoard(parseLevel(mNum))
        mViewModel.mBoard.observe(this) { board ->
            /*
             * 只有空格需要候选数标记对象，题目给定数字不允许添加标记。
             */
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (board[i][j].mValue == "0" && mTagData[i][j] == null) {
                        mTagData[i][j] = TagData()
                    }
                }
            }
            mBroadView.initData(board)
            mBroadView.initTagData(mTagData)
            mBroadView.invalidate()
        }
        mBroadView.setListener(object : BroadView.Listener {
            override fun onTouch(row: Int, col: Int, block: Int) {
                if (mViewModel.mCurrentBlock == 0 && mViewModel.mBoard.value?.get(row)?.get(col)?.mType == PlayViewModel.EMPTY) {
                    /*
                     * 首次点中空格时只更新选中状态，实际填数由数字按钮完成。
                     */
                }
                mViewModel.setCurrentPosition(row, col, block)
                mViewModel.selectCell(row, col)

                val cell = mViewModel.mBoard.value?.get(row)?.get(col) ?: return
                if (cell.mType == PlayViewModel.PROBLEM) {
                    for (numBtn in mNumbers) numBtn?.alpha = 0.55f
                    mTag.alpha = 0.55f
                } else if (cell.mValue != "0") {
                    for (numBtn in mNumbers) numBtn?.alpha = 1f
                    mTag.alpha = 0.55f
                } else {
                    mTag.alpha = 1f
                    if (mViewModel.mTagMode) {
                        val td = mTagData[row][col]
                        for (i in 0 until 9) {
                            mNumbers[i]?.alpha = if (td != null && td.haveTag((i + 1).toString())) 0.55f else 1f
                        }
                    } else {
                        for (numBtn in mNumbers) numBtn?.alpha = 1f
                    }
                }
                mBroadView.invalidate()
                PlayMusic.getInstance().playButtonTap()
            }
        })
    }

    private fun initTimer() {
        mViewModel.mRemainingSeconds.observe(this) { seconds ->
            val min = seconds / 60; val sec = seconds % 60
            mTimeProgressBar.progress = seconds
            mRemainingMins.text = String.format(Locale.ROOT, "%02d", min)
            mRemainingSecs.text = String.format(Locale.ROOT, "%02d", sec)

            if (seconds in 1..10) {
                val red = ContextCompat.getColor(this, R.color.red)
                mRemainingMins.setTextColor(red)
                mRemainingSecs.setTextColor(red)
                mColon.setTextColor(red)
                if (seconds <= 10) PlayMusic.getInstance().playTimesUp()
            } else {
                val white = ContextCompat.getColor(this, R.color.white)
                mRemainingMins.setTextColor(white)
                mRemainingSecs.setTextColor(white)
                mColon.setTextColor(white)
            }
        }

        mViewModel.mTimerFinished.observe(this) { finished ->
            if (finished) failed()
        }

        mViewModel.mHasWon.observe(this) { won ->
            if (won && !mIsPaused) {
                stopCountdownService()
                PlayMusic.getInstance().stopTimesUp()
                PlayMusic.getInstance().playWinning()
                playWinAnimation()
            }
        }

        mViewModel.mIsWrong.observe(this) { wrong ->
            if (wrong) {
                mBroadView.setWrong(true)
                PlayMusic.getInstance().playInputWrong()
                mViewModel.mCanInsert = false
                mHandler.postDelayed({
                    mViewModel.revertWrongInput(mViewModel.mCurrentX, mViewModel.mCurrentY)
                    mBroadView.initData(mViewModel.mBoard.value!!)
                    mBroadView.invalidate()
                    mBroadView.setWrong(false)
                    mViewModel.mCanInsert = true
                }, 200)
            }
        }
    }

    private fun playWinAnimation() {
        val board = mViewModel.mBoard.value ?: return
        for (i in 0 until 9)
            for (j in 0 until 9)
                board[i][j].mStatus = PlayViewModel.SELECT_NONE
        mBroadView.overDone(board)
        mBroadView.invalidate()

        val winningAnims = Array(9) { AnimatorSet() }
        for (i in 8 downTo 0) {
            winningAnims[i] = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofInt(mBroadView, "TextSize", 80, 100, 80),
                    ObjectAnimator.ofInt(mBroadView, "TextAlpha", 255, 0, 255),
                    ObjectAnimator.ofInt(mBroadView, "Line", i, i)
                )
                interpolator = LinearInterpolator()
                duration = 500
                startDelay = ((8 - i) * 200).toLong()
                start()
            }
        }

        mHandler.postDelayed({
            MyDialogManager.getInstance().show(mWinDialog)
            PlayMusic.getInstance().playGetStar()
            val winStar = mWinDialog.findViewById<ImageView>(R.id.win_star_on)
            ObjectAnimator.ofFloat(winStar, "alpha", 0f, 1f).setDuration(500).start()
            ObjectAnimator.ofFloat(winStar, "scaleX", 0.5f, 1.1f, 1f).setDuration(500).start()
            ObjectAnimator.ofFloat(winStar, "scaleY", 0.5f, 1.1f, 1f).setDuration(500).start()
        }, 2030)
    }

    private fun failed() {
        mBroadView.setWrong(true)
        PlayMusic.getInstance().stopTimesUp()
        PlayMusic.getInstance().playLosing()
        lifecycleScope.launch {
            saveAndQueryGameResultThroughProvider(parseLevel(mNum), completed = false)
        }
        mHandler.postDelayed({
            MyDialogManager.getInstance().show(mLoseDialog)
        }, 1500)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTagButton() {
        mTag.setOnTouchListener { v, event ->
            if (mViewModel.mCurrentBlock == 0 || mViewModel.mBoard.value?.get(mViewModel.mCurrentX)?.get(mViewModel.mCurrentY)?.mType == PlayViewModel.PROBLEM || mViewModel.mHasWon.value == true)
                return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            false
        }

        mTag.setOnClickListener {
            if (mViewModel.mHasWon.value == true) return@setOnClickListener
            val cell = mViewModel.mBoard.value?.get(mViewModel.mCurrentX)?.get(mViewModel.mCurrentY) ?: return@setOnClickListener
            if (cell.mType == PlayViewModel.PROBLEM) { PlayMusic.getInstance().playInputWrong(); return@setOnClickListener }
            if (mViewModel.mCurrentBlock == 0 || cell.mValue != "0") {
                PlayMusic.getInstance().playInputWrong()
                return@setOnClickListener
            }
            PlayMusic.getInstance().playButtonTap()
            if (!mViewModel.mTagMode) {
                mTag.setImageResource(R.drawable.icon_notes_on)
                mViewModel.mTagMode = true
                val td = mTagData[mViewModel.mCurrentX][mViewModel.mCurrentY]
                for (i in 0 until 9) {
                    mNumbers[i]?.alpha = if (td != null && td.haveTag((i + 1).toString())) 0.55f else 1f
                }
            } else {
                for (btn in mNumbers) btn?.alpha = 1f
                mTag.setImageResource(R.drawable.icon_notes_off)
                mViewModel.mTagMode = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initInsertButtons() {
        for (i in 0 until 9) {
            val idx = i
            mNumbers[i]?.setOnTouchListener { v, event ->
                if (mViewModel.mCurrentBlock == 0 || mViewModel.mBoard.value?.get(mViewModel.mCurrentX)?.get(mViewModel.mCurrentY)?.mType == PlayViewModel.PROBLEM || mViewModel.mHasWon.value == true)
                    return@setOnTouchListener false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
                false
            }

            mNumbers[i]?.setOnClickListener {
                if (mViewModel.mHasWon.value == true || !mViewModel.mCanInsert) return@setOnClickListener
                val cell = mViewModel.mBoard.value?.get(mViewModel.mCurrentX)?.get(mViewModel.mCurrentY) ?: return@setOnClickListener
                if (cell.mType == PlayViewModel.PROBLEM || mViewModel.mCurrentBlock == 0) {
                    PlayMusic.getInstance().playInputWrong(); return@setOnClickListener
                }
                val number = (idx + 1).toString()

                if (!mViewModel.mTagMode) {
                    lifecycleScope.launch {
                        /*
                         * 普通填数会写入撤销历史；如果填完后达成胜利，再写入公开战绩。
                         */
                        mViewModel.insertNumber(mViewModel.mCurrentX, mViewModel.mCurrentY, number)
                        mRevoke.alpha = 1f
                        mTag.alpha = 0.55f
                        mBroadView.initData(mViewModel.mBoard.value!!)
                        mBroadView.invalidate()

                        if (mViewModel.mHasWon.value == true) {
                            val levelNum = mNum.toInt()
                            mViewModel.updatePassStatus(levelNum, levelNum + 1)
                            saveAndQueryGameResultThroughProvider(levelNum, completed = true)
                        }
                    }
                } else {
                    /*
                     * 标记模式只修改候选数，不改变棋盘正式答案。
                     */
                    if (cell.mValue != "0") { PlayMusic.getInstance().playInputWrong(); return@setOnClickListener }
                    PlayMusic.getInstance().playButtonTap()
                    mRevoke.alpha = 1f
                    lifecycleScope.launch {
                        val added = mViewModel.insertOrRemoveTag(mViewModel.mCurrentX, mViewModel.mCurrentY, number, mTagData)
                        mNumbers[idx]?.alpha = if (added) 0.55f else 1f
                        mBroadView.initTagData(mTagData)
                        mBroadView.invalidate()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initRevokeButton() {
        mRevoke.alpha = 0.55f
        mRevoke.setOnTouchListener { v, event ->
            if (mViewModel.mCurrentBlock == 0 || mViewModel.mBoard.value?.get(mViewModel.mCurrentX)?.get(mViewModel.mCurrentY)?.mType == PlayViewModel.PROBLEM || mViewModel.mHasWon.value == true)
                return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            false
        }

        mRevoke.setOnClickListener {
            if (mViewModel.mHasWon.value == true || mViewModel.mCurrentBlock == 0) return@setOnClickListener
            lifecycleScope.launch {
                val history = mViewModel.undo()
                if (history == null) {
                    PlayMusic.getInstance().playInputWrong()
                    mRevoke.alpha = 0.55f
                    mTag.alpha = 1f
                    mTag.setImageResource(R.drawable.icon_notes_off)
                    mViewModel.mTagMode = false
                    mViewModel.mCurrentBlock = 0
                    val board = mViewModel.mBoard.value!!
                    for (i in 0 until 9)
                        for (j in 0 until 9)
                            board[i][j].mStatus = PlayViewModel.SELECT_NONE
                    mBroadView.initData(board)
                    mBroadView.invalidate()
                    return@launch
                }
                mViewModel.mCurrentX = history.mRow
                mViewModel.mCurrentY = history.mCol
                mViewModel.mCurrentBlock = mViewModel.mBoard.value?.get(history.mRow)?.get(history.mCol)?.mBlock ?: 0

                PlayMusic.getInstance().playButtonTap()

                if (history.mType == PlayViewModel.TYPE_NUMBER) {
                    mViewModel.mTagMode = false
                    mViewModel.mLastValue = history.mValue.toString()
                    mTag.setImageResource(R.drawable.icon_notes_off)
                    for (btn in mNumbers) btn?.alpha = 1f

                    val board = mViewModel.mBoard.value!!
                    board[history.mRow][history.mCol].mValue = history.mValue.toString()
                    board[history.mRow][history.mCol].mStatus = PlayViewModel.BE_SELECTED
                    if (history.mValue == 0) {
                        /*
                         * 撤销到空格时重新走一次选中逻辑，用于恢复同行、同列和同宫高亮。
                         */
                        mViewModel.selectCell(history.mRow, history.mCol)
                        mTag.alpha = 1f
                    } else {
                        mViewModel.selectCell(history.mRow, history.mCol)
                        mTag.alpha = 0.55f
                    }
                    mBroadView.initData(board)
                } else {
                    /*
                     * 标记撤销需要反向切换候选数，并同步数字按钮透明度。
                     */
                    val board = mViewModel.mBoard.value!!
                    board[history.mRow][history.mCol].mStatus = PlayViewModel.BE_SELECTED
                    mTag.alpha = 1f
                    mViewModel.mTagMode = true
                    mTag.setImageResource(R.drawable.icon_notes_on)

                    val td = mTagData[history.mRow][history.mCol]!!
                    val hVal = history.mValue.toString()
                    if (td.haveTag(hVal)) {
                        td.deleteTag(hVal)
                        mNumbers[history.mValue - 1]?.alpha = 1f
                    } else {
                        td.setTag(hVal)
                        mNumbers[history.mValue - 1]?.alpha = 0.55f
                    }
                    mBroadView.initTagData(mTagData)

                    for (i in 0 until 9) {
                        mNumbers[i]?.alpha = if (td.haveTag((i + 1).toString())) 0.55f else 1f
                    }
                    mBroadView.initData(board)
                }
                mBroadView.invalidate()
            }
        }
    }

    private fun initPauseButton() {
        mPauseButton.setOnClickListener {
            if (mViewModel.mHasWon.value == true) return@setOnClickListener
            PlayMusic.getInstance().playDialogShow()
            PlayMusic.getInstance().stopTimesUp()
            mIsPaused = true
            stopCountdownService()
            MyDialogManager.getInstance().show(mPauseDialog)
        }
    }

    private fun initDialogs(maxNum: Int) {
        /*
         * 暂停弹窗负责恢复、重开和返回地图；显示期间后台倒计时会停止。
         */
        mPauseDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_pause)
        mPauseDialog.setCanceledOnTouchOutside(false)

        mPauseDialog.findViewById<View>(R.id.pause_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mPauseDialog)
            mIsPaused = false
            startCountdownService()
        }

        mPauseDialog.findViewById<View>(R.id.pause_restart).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mPauseDialog)
            clearHistoryAndRun {
                finish()
                startActivityWithTransition(
                    Intent(this, PlayActivity::class.java).putExtra("mNum", mNum),
                    R.anim.playpage_show,
                    R.anim.playpage_hide
                )
            }
        }

        mPauseDialog.findViewById<View>(R.id.pause_back).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mPauseDialog)
            clearHistoryAndRun {
                startActivityWithTransition(
                    Intent(this, MapActivity::class.java)
                        .putExtra("roll", mNum)
                        .putExtra(MapActivity.EXTRA_FLASH_HOME, true),
                    R.anim.playpage_show,
                    R.anim.playpage_hide
                )
                finish()
            }
        }

        /*
         * 背景音乐开关写入偏好设置，重启页面后仍保持用户选择。
         */
        val musicBtn = mPauseDialog.findViewById<ImageView>(R.id.pause_music)
        musicBtn.setImageResource(if (mMusicOpened) R.drawable.icon_music_on else R.drawable.icon_music_off)
        musicBtn.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (mMusicOpened) {
                musicBtn.setImageResource(R.drawable.icon_music_off)
                mMusicOpened = false; prefs.edit { putBoolean("music", false) }
                PlayMusic.getInstance().stopBGM()
            } else {
                musicBtn.setImageResource(R.drawable.icon_music_on)
                mMusicOpened = true; prefs.edit { putBoolean("music", true) }
                PlayMusic.getInstance().playBGM()
            }
        }

        /*
         * 音效开关与背景音乐分开保存，允许用户只关闭按键和结果音效。
         */
        val audioBtn = mPauseDialog.findViewById<ImageView>(R.id.pause_audio)
        audioBtn.setImageResource(if (mAudioOpened) R.drawable.icon_sound_on else R.drawable.icon_sound_off)
        audioBtn.setOnClickListener {
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (mAudioOpened) {
                audioBtn.setImageResource(R.drawable.icon_sound_off)
                mAudioOpened = false; prefs.edit { putBoolean("audio", false) }
            } else {
                audioBtn.setImageResource(R.drawable.icon_sound_on)
                mAudioOpened = true; prefs.edit { putBoolean("audio", true) }
                PlayMusic.getInstance().playButtonTap()
            }
        }

        /*
         * 失败弹窗只提供返回地图和重试，避免超时后继续修改棋盘。
         */
        mLoseDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_lose)
        mLoseDialog.setCanceledOnTouchOutside(false)

        mLoseDialog.findViewById<View>(R.id.lose_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            clearHistoryAndRun {
                startActivityWithTransition(
                    Intent(this, MapActivity::class.java)
                        .putExtra("lose", mNum)
                        .putExtra(MapActivity.EXTRA_FLASH_HOME, true),
                    R.anim.playpage_show,
                    R.anim.playpage_hide
                )
                finish()
            }
        }

        mLoseDialog.findViewById<View>(R.id.lose_retry).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            clearHistoryAndRun {
                finish()
                startActivityWithTransition(
                    Intent(this, PlayActivity::class.java).putExtra("mNum", mNum),
                    R.anim.playpage_show,
                    R.anim.playpage_hide
                )
            }
        }

        /*
         * 胜利弹窗负责进入下一关或返回地图，通关状态已在显示前写入数据库。
         */
        mWinDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_win)
        mWinDialog.setCanceledOnTouchOutside(false)

        mWinDialog.findViewById<View>(R.id.win_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            clearHistoryAndRun {
                val level = parseLevel(mNum)
                val intent = Intent(this, MapActivity::class.java)
                    .putExtra(MapActivity.EXTRA_FLASH_HOME, true)
                if (level < maxNum) intent.putExtra("next", (level + 1).toString())
                startActivityWithTransition(intent, R.anim.playpage_show, R.anim.playpage_hide)
                finish()
            }
        }

        mWinDialog.findViewById<View>(R.id.win_next).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            clearHistoryAndRun {
                finish()
                val level = parseLevel(mNum)
                if (level == maxNum) {
                    startActivityWithTransition(
                        Intent(this, MapActivity::class.java)
                            .putExtra(MapActivity.EXTRA_FLASH_HOME, true),
                        R.anim.playpage_show,
                        R.anim.playpage_hide
                    )
                } else {
                    startActivityWithTransition(
                        Intent(this, PlayActivity::class.java)
                            .putExtra("mNum", (level + 1).toString()),
                        R.anim.playpage_show,
                        R.anim.playpage_hide
                    )
                }
            }
        }
    }

    private fun initBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                mIsPaused = true
                stopCountdownService()
                PlayMusic.getInstance().stopTimesUp()
                PlayMusic.getInstance().playDialogShow()
                MyDialogManager.getInstance().show(mPauseDialog)
            }
        })
    }

    override fun onStart() {
        super.onStart()
        if (!mCountdownReceiverRegistered) {
            /*
             * 动态注册接收器，生命周期跟随页面可见状态，避免静态接收器长期占用资源。
             */
            ContextCompat.registerReceiver(
                this,
                mCountdownReceiver,
                IntentFilter(CountdownTimerContract.ACTION_COUNTDOWN_TICK),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            mCountdownReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        mIsPaused = true
        PlayMusic.getInstance().stopBGM()
        PlayMusic.getInstance().stopTimesUp()
        stopCountdownService()
        if (mViewModel.mHasWon.value == true) {
            PlayMusic.getInstance().stopWinning()
        }
    }

    override fun onStop() {
        if (mCountdownReceiverRegistered) {
            /*
             * 页面不可见时立即解绑接收器，防止页面销毁后仍收到倒计时广播。
             */
            unregisterReceiver(mCountdownReceiver)
            mCountdownReceiverRegistered = false
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        PlayMusic.getInstance().playBGM()
        if (mViewModel.mHasWon.value == true) {
            MyDialogManager.getInstance().show(mWinDialog)
        } else if (mViewModel.mTimerFinished.value == true) {
            MyDialogManager.getInstance().show(mLoseDialog)
        } else if (mIsPaused && !mPauseDialog.isShowing) {
            MyDialogManager.getInstance().show(mPauseDialog)
        } else if (!mIsPaused) {
            startCountdownService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        stopCountdownService()
        MyDialogManager.getInstance().hide(mPauseDialog)
        MyDialogManager.getInstance().hide(mWinDialog)
        MyDialogManager.getInstance().hide(mLoseDialog)
    }

    private fun startCountdownService() {
        if (mViewModel.mHasWon.value == true || mViewModel.mTimerFinished.value == true) return
        /*
         * 页面启动后台服务，但不直接倒计时；
         * 数据流为：后台服务发送广播，接收器更新视图模型，界面观察数据变化后刷新。
         */
        startService(
            Intent(this, CountdownTimerService::class.java)
                .putExtra(CountdownTimerContract.EXTRA_INITIAL_SECONDS, mViewModel.getRemainingSeconds())
        )
    }

    private fun stopCountdownService() {
        stopService(Intent(this, CountdownTimerService::class.java))
    }

    private suspend fun saveAndQueryGameResultThroughProvider(levelNum: Int, completed: Boolean) {
        if (mResultRecorded) return
        val remainingSeconds = mViewModel.getRemainingSeconds()
        val elapsedSeconds = (CountdownTimerContract.DEFAULT_TOTAL_SECONDS - remainingSeconds).coerceAtLeast(0)
        /*
         * 页面作为内容提供器客户端，通过内容解析器写入和查询战绩；
         * 外部应用也可使用同一地址读取玩家公开战绩。
         */
        val saved = withContext(Dispatchers.IO) {
            try {
                val values = GameResultContract.Results.toContentValues(
                    level = levelNum,
                    elapsedSeconds = elapsedSeconds,
                    remainingSeconds = remainingSeconds,
                    completed = completed
                )
                val insertedUri = contentResolver.insert(GameResultContract.Results.CONTENT_URI, values)
                if (insertedUri != null) {
                    contentResolver.query(insertedUri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            /*
                             * 查询示例保留字段读取，证明页面使用的是标准内容提供器接口；
                             * 当前界面不展示这些值，后续可接排行榜或成就页。
                             */
                            cursor.getInt(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_LEVEL))
                            cursor.getInt(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_ELAPSED_SECONDS))
                            cursor.getInt(cursor.getColumnIndexOrThrow(GameResultContract.Results.COLUMN_COMPLETED))
                        }
                    }
                    true
                } else {
                    false
                }
            } catch (e: RuntimeException) {
                Log.w(TAG, "通过内容提供器保存战绩失败", e)
                false
            }
        }
        if (saved) {
            mResultRecorded = true
        }
    }

    private fun parseLevel(raw: String?): Int {
        return raw?.toIntOrNull()?.takeIf { it in 1..MAX_LEVEL } ?: 1
    }

    private fun clearHistoryAndRun(action: () -> Unit) {
        lifecycleScope.launch {
            mViewModel.clearHistory()
            action()
        }
    }
}
