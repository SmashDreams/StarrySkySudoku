package com.bird.starryskysudoku.ui.map

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.howtoplay.HowToPlayActivity
import com.bird.starryskysudoku.ui.play.PlayActivity
import com.bird.starryskysudoku.ui.splash.SplashActivity

class MapActivity : AppCompatActivity() {

    private lateinit var settings: ImageView
    private lateinit var bigShootingStar: ImageView
    private lateinit var smallShootingStar: ImageView
    private lateinit var backgroundStars: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PassListAdapter
    private lateinit var viewModel: MapViewModel

    private var musicOpened = true
    private var audioOpened = true
    private var language = "zh"
    private var nextNum: String? = null
    private var loseNum: String? = null
    private var delayTime = 0
    private var lightStars = 0
    private var backPressCount = 0

    private lateinit var settingsDialog: MyDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mappage)

        nextNum = intent.getStringExtra("next")
        loseNum = intent.getStringExtra("lose")

        val db = DatabaseInitializer.getDatabase(this)
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MapViewModel(db) as T
            }
        })[MapViewModel::class.java]

        val musicPrefs = getSharedPreferences("music_set", MODE_PRIVATE)
        musicOpened = musicPrefs.getBoolean("music", true)
        audioOpened = musicPrefs.getBoolean("audio", true)
        language = getSharedPreferences("language", MODE_PRIVATE).getString("language", "zh") ?: "zh"

        recyclerView = findViewById(R.id.pass_list)
        bigShootingStar = findViewById(R.id.sstar_big)
        smallShootingStar = findViewById(R.id.sstar_small)
        backgroundStars = findViewById(R.id.map_bgstar)
        settings = findViewById(R.id.settings)

        initMapData()
        initList()
        initSettingDialog()
        initShootingStar()
        PlayMusic.getInstance().playBGM()
    }

    private fun initMapData() {
        viewModel.loadMapData()

        val rollTo = intent.getStringExtra("roll")
        if (rollTo != null) recyclerView.scrollToPosition(getRollingPosition(rollTo))

        if (nextNum != null) { delayTime = 1050; handleCheckNum(nextNum!!) }
        if (loseNum != null) {
            delayTime = 500
            handleCheckNum(loseNum!!)
            if (nextNum == null) recyclerView.scrollToPosition(getRollingPosition(loseNum!!))
        }
    }

    private fun handleCheckNum(num: String) {
        viewModel.getPassStatus(num.toInt()) { status ->
            when (status) {
                "待通关" -> openPassCheck(num)
                "已通关" -> openRetryCheck(num)
            }
        }
    }

    private fun openPassCheck(checkNum: String) {
        if (nextNum != null) {
            lightStars = nextNum!!.toInt() - 1
            Handler(Looper.getMainLooper()).postDelayed({
                if (nextNum!!.toInt() % 4 == 0) {
                    recyclerView.smoothScrollBy(0, -400)
                    delayTime = 1550
                }
            }, 700)
        }

        val dialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_passcheck).apply {
            findViewById<TextView>(R.id.passcheck_num).text = checkNum
            findViewById<TextView>(R.id.passcheck_passtimes).text = "0"
            findViewById<ImageView>(R.id.passcheck_star).setImageResource(R.drawable.ic_pop_star_bg)

            findViewById<View>(R.id.passcheck_close).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    MyDialogManager.getInstance().hide(this)
                }, 200)
            }

            findViewById<View>(R.id.passcheck_start).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this@MapActivity, PlayActivity::class.java)
                        .putExtra("num", checkNum))
                    overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
                    finish()
                    MyDialogManager.getInstance().hide(this)
                }, 165)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            MyDialogManager.getInstance().show(dialog)
        }, delayTime.toLong())
    }

    private fun openRetryCheck(checkNum: String) {
        val dialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_passcheck).apply {
            findViewById<ImageView>(R.id.passcheck_star).setImageResource(R.drawable.ic_pop_star_bg)
            findViewById<TextView>(R.id.passcheck_num).text = checkNum

            viewModel.getPassTimes(checkNum.toInt()) { times ->
                findViewById<TextView>(R.id.passcheck_passtimes).text = times
            }

            findViewById<View>(R.id.passcheck_close).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    MyDialogManager.getInstance().hide(this)
                }, 200)
            }

            findViewById<View>(R.id.passcheck_start).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                Handler(Looper.getMainLooper()).postDelayed({
                    intent.removeExtra("next")
                    intent.removeExtra("lose")
                    startActivity(Intent(this@MapActivity, PlayActivity::class.java)
                        .putExtra("num", checkNum))
                    overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
                    MyDialogManager.getInstance().hide(this)
                }, 165)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            MyDialogManager.getInstance().show(dialog)
        }, delayTime.toLong())
    }

    private fun initList() {
        recyclerView.layoutManager = LinearLayoutManager(this@MapActivity)
        // Set empty adapter immediately to avoid "No adapter attached" warning
        adapter = PassListAdapter(emptyList(), lightStars)
        recyclerView.adapter = adapter

        viewModel.mapData.observe(this) { data ->
            adapter = PassListAdapter(data, lightStars)
            recyclerView.adapter = adapter
            recyclerView.scrollToPosition(adapter.getPosition())
            recyclerView.smoothScrollBy(0, 150)

            adapter.setOpenListener(object : PassListAdapter.OpenPlayPage {
                override fun onOpen(num: String) {
                    startActivity(Intent(this@MapActivity, PlayActivity::class.java)
                        .putExtra("num", num))
                    overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
                    finish()
                }
            })
        }
    }

    private fun initSettingDialog() {
        settingsDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_settings)
        settingsDialog.setCanceledOnTouchOutside(true)

        val musicSwitch = settingsDialog.findViewById<ImageView>(R.id.settings_music)
        val audioSwitch = settingsDialog.findViewById<ImageView>(R.id.settings_audio)

        settings.setOnClickListener {
            PlayMusic.getInstance().playDialogShow()
            musicSwitch.setImageResource(
                if (musicOpened) R.drawable.ic_pop_music_on else R.drawable.ic_pop_music_off
            )
            audioSwitch.setImageResource(
                if (audioOpened) R.drawable.ic_pop_audio_on else R.drawable.ic_pop_audio_off
            )
            MyDialogManager.getInstance().show(settingsDialog)
        }

        settingsDialog.findViewById<View>(R.id.settings_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(settingsDialog)
        }

        settingsDialog.findViewById<View>(R.id.settings_howtoplay).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            startActivity(Intent(this, HowToPlayActivity::class.java))
            overridePendingTransition(R.anim.setguide_right_in, R.anim.mappage_gone)
        }

        settingsDialog.findViewById<View>(R.id.settings_language).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val newLang = if (language == "zh") "en" else "zh"
            getSharedPreferences("language", MODE_PRIVATE).edit()
                .putString("language", newLang).apply()
            startActivity(Intent(this, SplashActivity::class.java))
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
            finishAffinity()
        }

        musicSwitch.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (musicOpened) {
                musicSwitch.setImageResource(R.drawable.ic_pop_music_off)
                musicOpened = false
                prefs.edit().putBoolean("music", false).apply()
                PlayMusic.getInstance().stopBGM()
            } else {
                musicSwitch.setImageResource(R.drawable.ic_pop_music_on)
                musicOpened = true
                prefs.edit().putBoolean("music", true).apply()
                PlayMusic.getInstance().playBGM()
            }
        }

        audioSwitch.setOnClickListener {
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (audioOpened) {
                audioSwitch.setImageResource(R.drawable.ic_pop_audio_off)
                audioOpened = false
                prefs.edit().putBoolean("audio", false).apply()
            } else {
                audioSwitch.setImageResource(R.drawable.ic_pop_audio_on)
                audioOpened = true
                prefs.edit().putBoolean("audio", true).apply()
                PlayMusic.getInstance().playButtonTap()
            }
        }
    }

    private fun initShootingStar() {
        bigShootingStar.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shootingstar_big))
        smallShootingStar.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shootingstar_small))
        ObjectAnimator.ofFloat(backgroundStars, "alpha", 0.5f, 1f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backPressCount == 1) finishAffinity()
            else {
                Toast.makeText(this, R.string.pressagain, Toast.LENGTH_SHORT).show()
                backPressCount++
                Handler(Looper.getMainLooper()).postDelayed({ backPressCount = 0 }, 1500)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun getRollingPosition(num: String): Int {
        val position = num.toIntOrNull() ?: return 1
        val n = (position - 1) / 4
        return if (n in 0..8) 10 - n else 1
    }

    override fun onPause() { super.onPause(); PlayMusic.getInstance().stopBGM() }

    override fun onResume() {
        super.onResume()
        PlayMusic.getInstance().playBGM()
        viewModel.loadMapData()
        intent.getStringExtra("roll")?.let { recyclerView.scrollToPosition(getRollingPosition(it)) }
        nextNum = intent.getStringExtra("next")
        loseNum = intent.getStringExtra("lose")
        intent.removeExtra("next")
        intent.removeExtra("lose")
        nextNum?.let { delayTime = 1050; handleCheckNum(it) }
        loseNum?.let { delayTime = 500; handleCheckNum(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        MyDialogManager.getInstance().hide(settingsDialog)
    }
}
