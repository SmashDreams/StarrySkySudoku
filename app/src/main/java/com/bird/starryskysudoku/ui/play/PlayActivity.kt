package com.bird.starryskysudoku.ui.play

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.ui.map.MapActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var viewModel: PlayViewModel
    private lateinit var broadView: BroadView

    // UI elements
    private lateinit var playNum: TextView
    private lateinit var timeProgressBar: ProgressBar
    private lateinit var remainingMins: TextView
    private lateinit var remainingSecs: TextView
    private lateinit var colon: TextView
    private val numbers = arrayOfNulls<TextView>(9)
    private lateinit var revoke: ImageView
    private lateinit var tag: ImageView
    private lateinit var pauseButton: ImageView

    // State
    private var musicOpened = true
    private var audioOpened = true
    private var isPaused = false
    private var num = "1"
    private var tagData = Array(9) { arrayOfNulls<TagData>(9) }

    // Dialogs
    private lateinit var pauseDialog: MyDialog
    private lateinit var winDialog: MyDialog
    private lateinit var loseDialog: MyDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        num = intent.getStringExtra("num") ?: "1"
        val maxNum = 40

        val db = DatabaseInitializer.getDatabase(this)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlayViewModel(db) as T
            }
        })[PlayViewModel::class.java]

        val musicPrefs = getSharedPreferences("music_set", MODE_PRIVATE)
        musicOpened = musicPrefs.getBoolean("music", true)
        audioOpened = musicPrefs.getBoolean("audio", true)

        // Bind views
        playNum = findViewById(R.id.play_num); playNum.text = num
        timeProgressBar = findViewById(R.id.play_time_progressbar)
        remainingMins = findViewById(R.id.play_time_min)
        remainingSecs = findViewById(R.id.play_time_sec)
        colon = findViewById(R.id.textview74)
        broadView = findViewById(R.id.play_broad)
        numbers[0] = findViewById(R.id.play_1); numbers[1] = findViewById(R.id.play_2)
        numbers[2] = findViewById(R.id.play_3); numbers[3] = findViewById(R.id.play_4)
        numbers[4] = findViewById(R.id.play_5); numbers[5] = findViewById(R.id.play_6)
        numbers[6] = findViewById(R.id.play_7); numbers[7] = findViewById(R.id.play_8)
        numbers[8] = findViewById(R.id.play_9)
        revoke = findViewById(R.id.play_revoke); tag = findViewById(R.id.play_tag)
        pauseButton = findViewById(R.id.play_pause)

        timeProgressBar.max = 600

        initBoard()
        initTimer()
        initDialogs(maxNum)
        initTagButton()
        initInsertButtons()
        initRevokeButton()
        initPauseButton()
    }

    private fun initBoard() {
        viewModel.initBoard(num.toInt())
        viewModel.board.observe(this) { board ->
            // Initialize tag data for empty cells
            for (i in 0 until 9) {
                for (j in 0 until 9) {
                    if (board[i][j].value == "0" && tagData[i][j] == null) {
                        tagData[i][j] = TagData()
                    }
                }
            }
            broadView.initData(board)
            broadView.initTagData(tagData)
            broadView.invalidate()
        }
        broadView.setListener(object : BroadView.Listener {
            override fun onTouch(row: Int, col: Int, block: Int) {
                if (viewModel.currentBlock == 0 && viewModel.board.value?.get(row)?.get(col)?.type == PlayViewModel.EMPTY) {
                    // already selected empty - proceed
                }
                viewModel.setCurrentPosition(row, col, block)
                viewModel.selectCell(row, col)

                val cell = viewModel.board.value?.get(row)?.get(col) ?: return
                if (cell.type == PlayViewModel.PROBLEM) {
                    for (numBtn in numbers) numBtn?.alpha = 0.55f
                    tag.alpha = 0.55f
                } else if (cell.value != "0") {
                    for (numBtn in numbers) numBtn?.alpha = 1f
                    tag.alpha = 0.55f
                } else {
                    tag.alpha = 1f
                    if (viewModel.tagMode) {
                        val td = tagData[row][col]
                        for (i in 0 until 9) {
                            numbers[i]?.alpha = if (td != null && td.haveTag((i + 1).toString())) 0.55f else 1f
                        }
                    } else {
                        for (numBtn in numbers) numBtn?.alpha = 1f
                    }
                }
                broadView.invalidate()
                PlayMusic.getInstance().playButtonTap()
            }
        })
    }

    private fun initTimer() {
        viewModel.remainingSeconds.observe(this) { seconds ->
            val min = seconds / 60; val sec = seconds % 60
            timeProgressBar.progress = seconds
            remainingMins.text = String.format("%02d", min)
            remainingSecs.text = String.format("%02d", sec)

            if (seconds in 1..10) {
                val red = getColor(R.color.red)
                remainingMins.setTextColor(red)
                remainingSecs.setTextColor(red)
                colon.setTextColor(red)
                if (seconds <= 10) PlayMusic.getInstance().playTimesUp()
            } else {
                val white = getColor(R.color.white)
                remainingMins.setTextColor(white)
                remainingSecs.setTextColor(white)
                colon.setTextColor(white)
            }
        }

        viewModel.timerFinished.observe(this) { finished ->
            if (finished) failed()
        }

        viewModel.startTimer()

        viewModel.hasWon.observe(this) { won ->
            if (won && !isPaused) {
                viewModel.pauseTimer()
                PlayMusic.getInstance().stopTimesUp()
                PlayMusic.getInstance().playWinning()
                playWinAnimation()
            }
        }

        viewModel.isWrong.observe(this) { wrong ->
            if (wrong) {
                broadView.setWrong(true)
                PlayMusic.getInstance().playInputWrong()
                viewModel.canInsert = false
                handler.postDelayed({
                    viewModel.revertWrongInput(viewModel.currentX, viewModel.currentY)
                    broadView.initData(viewModel.board.value!!)
                    broadView.invalidate()
                    broadView.setWrong(false)
                    viewModel.canInsert = true
                }, 200)
            }
        }
    }

    private fun playWinAnimation() {
        val board = viewModel.board.value ?: return
        for (i in 0 until 9)
            for (j in 0 until 9)
                board[i][j].status = PlayViewModel.SELECT_NONE
        broadView.overDone(board)
        broadView.invalidate()

        val winningAnims = Array(9) { AnimatorSet() }
        for (i in 8 downTo 0) {
            winningAnims[i] = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofInt(broadView, "TextSize", 80, 100, 80),
                    ObjectAnimator.ofInt(broadView, "TextAlpha", 255, 0, 255),
                    ObjectAnimator.ofInt(broadView, "Line", i, i)
                )
                interpolator = LinearInterpolator()
                duration = 500
                startDelay = ((8 - i) * 200).toLong()
                start()
            }
        }

        handler.postDelayed({
            MyDialogManager.getInstance().show(winDialog)
            PlayMusic.getInstance().playGetStar()
            val winStar = winDialog.findViewById<ImageView>(R.id.win_star_on)
            ObjectAnimator.ofFloat(winStar, "alpha", 0f, 1f).setDuration(500).start()
            ObjectAnimator.ofFloat(winStar, "scaleX", 0.5f, 1.1f, 1f).setDuration(500).start()
            ObjectAnimator.ofFloat(winStar, "scaleY", 0.5f, 1.1f, 1f).setDuration(500).start()
        }, 2030)
    }

    private fun failed() {
        broadView.setWrong(true)
        PlayMusic.getInstance().stopTimesUp()
        PlayMusic.getInstance().playLosing()
        handler.postDelayed({
            MyDialogManager.getInstance().show(loseDialog)
        }, 1500)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initTagButton() {
        tag.setOnTouchListener { v, event ->
            if (viewModel.currentBlock == 0 || viewModel.board.value?.get(viewModel.currentX)?.get(viewModel.currentY)?.type == PlayViewModel.PROBLEM || viewModel.hasWon.value == true)
                return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            false
        }

        tag.setOnClickListener {
            if (viewModel.hasWon.value == true) return@setOnClickListener
            val cell = viewModel.board.value?.get(viewModel.currentX)?.get(viewModel.currentY) ?: return@setOnClickListener
            if (cell.type == PlayViewModel.PROBLEM) { PlayMusic.getInstance().playInputWrong(); return@setOnClickListener }
            PlayMusic.getInstance().playButtonTap()
            if (!viewModel.tagMode) {
                tag.setImageResource(R.drawable.ic_play_numberbox_markon)
                viewModel.tagMode = true
                if (cell.value != "0") return@setOnClickListener
                val td = tagData[viewModel.currentX][viewModel.currentY]
                for (i in 0 until 9) {
                    numbers[i]?.alpha = if (td != null && td.haveTag((i + 1).toString())) 0.55f else 1f
                }
            } else {
                for (btn in numbers) btn?.alpha = 1f
                tag.setImageResource(R.drawable.ic_play_numberbox_markoff)
                viewModel.tagMode = false
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initInsertButtons() {
        for (i in 0 until 9) {
            val idx = i
            numbers[i]?.setOnTouchListener { v, event ->
                if (viewModel.currentBlock == 0 || viewModel.board.value?.get(viewModel.currentX)?.get(viewModel.currentY)?.type == PlayViewModel.PROBLEM || viewModel.hasWon.value == true)
                    return@setOnTouchListener false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }
                false
            }

            numbers[i]?.setOnClickListener {
                if (viewModel.hasWon.value == true || !viewModel.canInsert) return@setOnClickListener
                val cell = viewModel.board.value?.get(viewModel.currentX)?.get(viewModel.currentY) ?: return@setOnClickListener
                if (cell.type == PlayViewModel.PROBLEM || viewModel.currentBlock == 0) {
                    PlayMusic.getInstance().playInputWrong(); return@setOnClickListener
                }
                val number = (idx + 1).toString()

                if (!viewModel.tagMode) {
                    lifecycleScope.launch {
                        viewModel.insertNumber(viewModel.currentX, viewModel.currentY, number)
                        revoke.alpha = 1f
                        tag.alpha = 0.55f
                        broadView.initData(viewModel.board.value!!)
                        broadView.invalidate()

                        if (viewModel.hasWon.value == true) {
                            val levelNum = num.toInt()
                            viewModel.updatePassStatus(levelNum, levelNum + 1)
                        }
                    }
                } else {
                    // Tag mode
                    if (cell.value != "0") { PlayMusic.getInstance().playInputWrong(); return@setOnClickListener }
                    PlayMusic.getInstance().playButtonTap()
                    revoke.alpha = 1f
                    lifecycleScope.launch {
                        val added = viewModel.insertOrRemoveTag(viewModel.currentX, viewModel.currentY, number, tagData)
                        numbers[idx]?.alpha = if (added) 0.55f else 1f
                        broadView.initTagData(tagData)
                        broadView.invalidate()
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initRevokeButton() {
        revoke.alpha = 0.55f
        revoke.setOnTouchListener { v, event ->
            if (viewModel.currentBlock == 0 || viewModel.board.value?.get(viewModel.currentX)?.get(viewModel.currentY)?.type == PlayViewModel.PROBLEM || viewModel.hasWon.value == true)
                return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }
            false
        }

        revoke.setOnClickListener {
            if (viewModel.hasWon.value == true || viewModel.currentBlock == 0) return@setOnClickListener
            lifecycleScope.launch {
                val history = viewModel.undo()
                if (history == null) {
                    PlayMusic.getInstance().playInputWrong()
                    revoke.alpha = 0.55f
                    tag.alpha = 1f
                    tag.setImageResource(R.drawable.ic_play_numberbox_markoff)
                    viewModel.tagMode = false
                    viewModel.currentBlock = 0
                    val board = viewModel.board.value!!
                    for (i in 0 until 9)
                        for (j in 0 until 9)
                            board[i][j].status = PlayViewModel.SELECT_NONE
                    broadView.initData(board)
                    broadView.invalidate()
                    return@launch
                }
                viewModel.currentX = history.row
                viewModel.currentY = history.col
                viewModel.currentBlock = viewModel.board.value?.get(history.row)?.get(history.col)?.block ?: 0

                PlayMusic.getInstance().playButtonTap()

                if (history.type == PlayViewModel.TYPE_NUMBER) {
                    viewModel.tagMode = false
                    viewModel.lastValue = history.value.toString()
                    tag.setImageResource(R.drawable.ic_play_numberbox_markoff)
                    for (btn in numbers) btn?.alpha = 1f

                    val board = viewModel.board.value!!
                    board[history.row][history.col].value = history.value.toString()
                    board[history.row][history.col].status = PlayViewModel.BE_SELECTED
                    if (history.value == 0) {
                        viewModel.selectCell(history.row, history.col) // will tap empty
                        tag.alpha = 1f
                    } else {
                        viewModel.selectCell(history.row, history.col)
                        tag.alpha = 0.55f
                    }
                    broadView.initData(board)
                } else {
                    // Tag type undo
                    val board = viewModel.board.value!!
                    board[history.row][history.col].status = PlayViewModel.BE_SELECTED
                    tag.alpha = 1f
                    viewModel.tagMode = true
                    tag.setImageResource(R.drawable.ic_play_numberbox_markon)

                    val td = tagData[history.row][history.col]!!
                    val hVal = history.value.toString()
                    if (td.haveTag(hVal)) {
                        td.deleteTag(hVal)
                        numbers[history.value - 1]?.alpha = 1f
                    } else {
                        td.setTag(hVal)
                        numbers[history.value - 1]?.alpha = 0.55f
                    }
                    broadView.initTagData(tagData)

                    for (i in 0 until 9) {
                        numbers[i]?.alpha = if (td.haveTag((i + 1).toString())) 0.55f else 1f
                    }
                    broadView.initData(board)
                }
                broadView.invalidate()
            }
        }
    }

    private fun initPauseButton() {
        pauseButton.setOnClickListener {
            if (viewModel.hasWon.value == true) return@setOnClickListener
            PlayMusic.getInstance().playDialogShow()
            PlayMusic.getInstance().stopTimesUp()
            viewModel.pauseTimer()
            MyDialogManager.getInstance().show(pauseDialog)
        }
    }

    private fun initDialogs(maxNum: Int) {
        // Pause dialog
        pauseDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_pause)
        pauseDialog.setCanceledOnTouchOutside(false)

        pauseDialog.findViewById<View>(R.id.pause_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(pauseDialog)
            viewModel.startTimer()
        }

        pauseDialog.findViewById<View>(R.id.pause_restart).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(pauseDialog)
            finish()
            startActivity(Intent(this, PlayActivity::class.java).putExtra("num", num))
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
        }

        pauseDialog.findViewById<View>(R.id.pause_back).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(pauseDialog)
            lifecycleScope.launch { viewModel.clearHistory() }
            startActivity(Intent(this, MapActivity::class.java).putExtra("roll", num))
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
            finish()
        }

        // Music toggle in pause dialog
        val musicBtn = pauseDialog.findViewById<ImageView>(R.id.pause_music)
        musicBtn.setImageResource(if (musicOpened) R.drawable.ic_pop_music_on else R.drawable.ic_pop_music_off)
        musicBtn.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (musicOpened) {
                musicBtn.setImageResource(R.drawable.ic_pop_music_off)
                musicOpened = false; prefs.edit().putBoolean("music", false).apply()
                PlayMusic.getInstance().stopBGM()
            } else {
                musicBtn.setImageResource(R.drawable.ic_pop_music_on)
                musicOpened = true; prefs.edit().putBoolean("music", true).apply()
                PlayMusic.getInstance().playBGM()
            }
        }

        // Audio toggle
        val audioBtn = pauseDialog.findViewById<ImageView>(R.id.pause_audio)
        audioBtn.setImageResource(if (audioOpened) R.drawable.ic_pop_audio_on else R.drawable.ic_pop_audio_off)
        audioBtn.setOnClickListener {
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (audioOpened) {
                audioBtn.setImageResource(R.drawable.ic_pop_audio_off)
                audioOpened = false; prefs.edit().putBoolean("audio", false).apply()
            } else {
                audioBtn.setImageResource(R.drawable.ic_pop_audio_on)
                audioOpened = true; prefs.edit().putBoolean("audio", true).apply()
                PlayMusic.getInstance().playButtonTap()
            }
        }

        // Lose dialog
        loseDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_lose)
        loseDialog.setCanceledOnTouchOutside(false)

        loseDialog.findViewById<View>(R.id.lose_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            startActivity(Intent(this, MapActivity::class.java).putExtra("lose", num))
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
            finish()
        }

        loseDialog.findViewById<View>(R.id.lose_retry).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            finish()
            startActivity(Intent(this, PlayActivity::class.java).putExtra("num", num))
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
        }

        // Win dialog
        winDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_win)
        winDialog.setCanceledOnTouchOutside(false)

        winDialog.findViewById<View>(R.id.win_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            startActivity(Intent(this, MapActivity::class.java)
                .putExtra("next", (num.toInt() + 1).toString()))
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
            finish()
        }

        winDialog.findViewById<View>(R.id.win_next).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            finish()
            if (num.toInt() == maxNum) {
                startActivity(Intent(this, MapActivity::class.java))
            } else {
                startActivity(Intent(this, PlayActivity::class.java)
                    .putExtra("num", (num.toInt() + 1).toString()))
            }
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            viewModel.pauseTimer()
            PlayMusic.getInstance().stopTimesUp()
            PlayMusic.getInstance().playDialogShow()
            MyDialogManager.getInstance().show(pauseDialog)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
        PlayMusic.getInstance().stopBGM()
        PlayMusic.getInstance().stopTimesUp()
        viewModel.pauseTimer()
        if (viewModel.hasWon.value == true) {
            PlayMusic.getInstance().stopWinning()
        }
    }

    override fun onResume() {
        super.onResume()
        PlayMusic.getInstance().playBGM()
        if (viewModel.hasWon.value == true) {
            MyDialogManager.getInstance().show(winDialog)
        } else if (viewModel.timerFinished.value == true) {
            MyDialogManager.getInstance().show(loseDialog)
        } else if (isPaused && !pauseDialog.isShowing) {
            MyDialogManager.getInstance().show(pauseDialog)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.pauseTimer()
        MyDialogManager.getInstance().hide(pauseDialog)
        MyDialogManager.getInstance().hide(winDialog)
        MyDialogManager.getInstance().hide(loseDialog)
    }
}
