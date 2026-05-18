package com.bird.starryskysudoku.ui.play

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.ui.map.MapActivity
import kotlinx.coroutines.launch
import java.util.Locale

class PlayActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LEVEL = 40
    }

    private val mHandler = Handler(Looper.getMainLooper())
    private lateinit var mViewModel: PlayViewModel
    private lateinit var mBroadView: BroadView

    // UI elements
    private lateinit var mPlayNum: TextView
    private lateinit var mTimeProgressBar: ProgressBar
    private lateinit var mRemainingMins: TextView
    private lateinit var mRemainingSecs: TextView
    private lateinit var mColon: TextView
    private val mNumbers = arrayOfNulls<TextView>(9)
    private lateinit var mRevoke: ImageView
    private lateinit var mTag: ImageView
    private lateinit var mPauseButton: ImageView

    // State
    private var mMusicOpened = true
    private var mAudioOpened = true
    private var mIsPaused = false
    private var mNum = "1"
    private var mTagData = Array(9) { arrayOfNulls<TagData>(9) }

    // Dialogs
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

        // Bind views
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

        mTimeProgressBar.max = 600

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
            // Initialize mTag data for empty cells
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
                    // already selected empty - proceed
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

        mViewModel.startTimer()

        mViewModel.mHasWon.observe(this) { won ->
            if (won && !mIsPaused) {
                mViewModel.pauseTimer()
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
                        mViewModel.insertNumber(mViewModel.mCurrentX, mViewModel.mCurrentY, number)
                        mRevoke.alpha = 1f
                        mTag.alpha = 0.55f
                        mBroadView.initData(mViewModel.mBoard.value!!)
                        mBroadView.invalidate()

                        if (mViewModel.mHasWon.value == true) {
                            val levelNum = mNum.toInt()
                            mViewModel.updatePassStatus(levelNum, levelNum + 1)
                        }
                    }
                } else {
                    // Tag mode
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
                        mViewModel.selectCell(history.mRow, history.mCol) // will tap empty
                        mTag.alpha = 1f
                    } else {
                        mViewModel.selectCell(history.mRow, history.mCol)
                        mTag.alpha = 0.55f
                    }
                    mBroadView.initData(board)
                } else {
                    // Tag type undo
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
            mViewModel.pauseTimer()
            MyDialogManager.getInstance().show(mPauseDialog)
        }
    }

    private fun initDialogs(maxNum: Int) {
        // Pause dialog
        mPauseDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_pause)
        mPauseDialog.setCanceledOnTouchOutside(false)

        mPauseDialog.findViewById<View>(R.id.pause_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mPauseDialog)
            mIsPaused = false
            mViewModel.startTimer()
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

        // Music toggle in pause dialog
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

        // Audio toggle
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

        // Lose dialog
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

        // Win dialog
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
                mViewModel.pauseTimer()
                PlayMusic.getInstance().stopTimesUp()
                PlayMusic.getInstance().playDialogShow()
                MyDialogManager.getInstance().show(mPauseDialog)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        mIsPaused = true
        PlayMusic.getInstance().stopBGM()
        PlayMusic.getInstance().stopTimesUp()
        mViewModel.pauseTimer()
        if (mViewModel.mHasWon.value == true) {
            PlayMusic.getInstance().stopWinning()
        }
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        mViewModel.pauseTimer()
        MyDialogManager.getInstance().hide(mPauseDialog)
        MyDialogManager.getInstance().hide(mWinDialog)
        MyDialogManager.getInstance().hide(mLoseDialog)
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
