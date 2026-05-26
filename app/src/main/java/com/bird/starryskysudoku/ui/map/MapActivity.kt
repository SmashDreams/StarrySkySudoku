package com.bird.starryskysudoku.ui.map

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.flashThreeTimes
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.ui.howtoplay.HowToPlayActivity
import com.bird.starryskysudoku.ui.play.PlayActivity

class MapActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LEVEL = 40
        const val EXTRA_FLASH_HOME = "flash_home"
        private const val PREFS_UI_STATE = "ui_state"
        private const val KEY_FLASH_HOME = "flash_home"
    }

    private lateinit var mSettings: ImageView
    private lateinit var mBigShootingStar: ImageView
    private lateinit var mSmallShootingStar: ImageView
    private lateinit var mBackgroundStars: ImageView
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLoginStatus: TextView
    private lateinit var mAdapter: PassListAdapter
    private lateinit var mViewModel: MapViewModel

    private var mMusicOpened = true
    private var mAudioOpened = true
    private var mLanguage = "zh"
    private var mNextNum: String? = null
    private var mLoseNum: String? = null
    private var mDelayTime = 0
    private var mLightStars = 0
    private var mBackPressCount = 0
    private var mCurrentUsername = LauncherSessionReader.GUEST_USERNAME
    private var mMapLoaded = false
    private val mHandler = Handler(Looper.getMainLooper())

    private lateinit var mSettingsDialog: MyDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mappage)

        val db = DatabaseInitializer.getDatabase(this)
        mViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MapViewModel(db) as T
            }
        })[MapViewModel::class.java]

        val musicPrefs = getSharedPreferences("music_set", MODE_PRIVATE)
        mMusicOpened = musicPrefs.getBoolean("music", true)
        mAudioOpened = musicPrefs.getBoolean("audio", true)
        mLanguage = getSharedPreferences("mLanguage", MODE_PRIVATE).getString("mLanguage", "zh") ?: "zh"

        mRecyclerView = findViewById(R.id.pass_list)
        mBigShootingStar = findViewById(R.id.sstar_big)
        mSmallShootingStar = findViewById(R.id.sstar_small)
        mBackgroundStars = findViewById(R.id.map_bgstar)
        mSettings = findViewById(R.id.settings)
        mLoginStatus = findViewById(R.id.login_status)
        mLoginStatus.setOnClickListener {
            Toast.makeText(this, R.string.login_prompt_teahouse, Toast.LENGTH_SHORT).show()
        }

        initList()
        initSettingDialog()
        initShootingStar()
        refreshLoginState()
        initMapData()
        initBackHandler()
        if (consumeHomeFlashRequest()) flashHome()
        PlayMusic.getInstance().playBGM()
    }

    private fun initMapData() {
        mMapLoaded = true
        mViewModel.loadMapData()
        consumeNavigationExtras()
    }

    private fun refreshLoginState() {
        val username = LauncherSessionReader.readUsername(contentResolver)
        mLoginStatus.text = if (username == LauncherSessionReader.GUEST_USERNAME) {
            getString(R.string.login_guest)
        } else {
            username
        }

        if (username != mCurrentUsername) {
            mCurrentUsername = username
            if (mMapLoaded) initMapData()
        }
    }

    private fun consumeNavigationExtras() {
        intent.getStringExtra("roll")?.let { mRecyclerView.scrollToPosition(getRollingPosition(it)) }
        mNextNum = intent.getStringExtra("next")?.takeIf { parseLevel(it) != null }
        mLoseNum = intent.getStringExtra("lose")?.takeIf { parseLevel(it) != null }
        intent.removeExtra("roll")
        intent.removeExtra("next")
        intent.removeExtra("lose")

        mNextNum?.let { mDelayTime = 1050; handleCheckNum(it) }
        mLoseNum?.let {
            mDelayTime = 500
            handleCheckNum(it)
            if (mNextNum == null) mRecyclerView.scrollToPosition(getRollingPosition(it))
        }
    }

    private fun handleCheckNum(num: String) {
        val passNum = parseLevel(num) ?: return
        mViewModel.getPassStatus(passNum) { status ->
            when (status) {
                "待通关" -> openPassCheck(passNum.toString())
                "已通关" -> openRetryCheck(passNum.toString())
            }
        }
    }

    private fun openPassCheck(checkNum: String) {
        if (mNextNum != null) {
            mLightStars = mNextNum!!.toInt() - 1
            mHandler.postDelayed({
                if (mNextNum!!.toInt() % 4 == 0) {
                    mRecyclerView.smoothScrollBy(0, -400)
                    mDelayTime = 1550
                }
            }, 700)
        }

        val dialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_passcheck).apply {
            findViewById<TextView>(R.id.passcheck_num).text = checkNum
            findViewById<TextView>(R.id.passcheck_passtimes).text = "0"
            findViewById<ImageView>(R.id.passcheck_star).setImageResource(R.drawable.star_empty)

            findViewById<View>(R.id.passcheck_close).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                mHandler.postDelayed({
                    MyDialogManager.getInstance().hide(this)
                }, 200)
            }

            findViewById<View>(R.id.passcheck_start).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                mHandler.postDelayed({
                    startActivityWithTransition(
                        Intent(this@MapActivity, PlayActivity::class.java)
                            .putExtra("num", checkNum),
                        R.anim.playpage_show,
                        R.anim.playpage_hide
                    )
                    finish()
                    MyDialogManager.getInstance().hide(this)
                }, 165)
            }
        }

        mHandler.postDelayed({
            MyDialogManager.getInstance().show(dialog)
        }, mDelayTime.toLong())
    }

    private fun openRetryCheck(checkNum: String) {
        val dialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_passcheck).apply {
            findViewById<ImageView>(R.id.passcheck_star).setImageResource(R.drawable.star_empty)
            findViewById<TextView>(R.id.passcheck_num).text = checkNum

            mViewModel.getPassTimes(checkNum.toInt()) { times ->
                findViewById<TextView>(R.id.passcheck_passtimes).text = times
            }

            findViewById<View>(R.id.passcheck_close).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                mHandler.postDelayed({
                    MyDialogManager.getInstance().hide(this)
                }, 200)
            }

            findViewById<View>(R.id.passcheck_start).setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                mHandler.postDelayed({
                    intent.removeExtra("next")
                    intent.removeExtra("lose")
                    startActivityWithTransition(
                        Intent(this@MapActivity, PlayActivity::class.java)
                            .putExtra("num", checkNum),
                        R.anim.playpage_show,
                        R.anim.playpage_hide
                    )
                    MyDialogManager.getInstance().hide(this)
                }, 165)
            }
        }

        mHandler.postDelayed({
            MyDialogManager.getInstance().show(dialog)
        }, mDelayTime.toLong())
    }

    private fun initList() {
        mRecyclerView.layoutManager = LinearLayoutManager(this@MapActivity)
        /*
         * 先设置空适配器，避免列表在数据加载前出现未绑定适配器警告。
         */
        mAdapter = PassListAdapter(emptyList(), mLightStars)
        mRecyclerView.adapter = mAdapter

        mViewModel.mMapData.observe(this) { data ->
            mAdapter = PassListAdapter(data, mLightStars)
            mRecyclerView.adapter = mAdapter
            mRecyclerView.scrollToPosition(mAdapter.getPosition())
            mRecyclerView.smoothScrollBy(0, 150)

            mAdapter.setOpenListener(object : PassListAdapter.OpenPlayPage {
                override fun onOpen(num: String) {
                    startActivityWithTransition(
                        Intent(this@MapActivity, PlayActivity::class.java)
                            .putExtra("num", num),
                        R.anim.playpage_show,
                        R.anim.playpage_hide
                    )
                    finish()
                }
            })
        }
    }

    private fun initSettingDialog() {
        mSettingsDialog = MyDialogManager.getInstance().initView(this, R.layout.dialog_settings)
        mSettingsDialog.setCanceledOnTouchOutside(true)

        val musicSwitch = mSettingsDialog.findViewById<ImageView>(R.id.settings_music)
        val audioSwitch = mSettingsDialog.findViewById<ImageView>(R.id.settings_audio)

        mSettings.setOnClickListener {
            PlayMusic.getInstance().playDialogShow()
            musicSwitch.setImageResource(
                if (mMusicOpened) R.drawable.icon_music_on else R.drawable.icon_music_off
            )
            audioSwitch.setImageResource(
                if (mAudioOpened) R.drawable.icon_sound_on else R.drawable.icon_sound_off
            )
            MyDialogManager.getInstance().show(mSettingsDialog)
        }

        mSettingsDialog.findViewById<View>(R.id.settings_close).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mSettingsDialog)
        }

        mSettingsDialog.findViewById<View>(R.id.settings_howtoplay).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            startActivityWithTransition(
                Intent(this, HowToPlayActivity::class.java),
                R.anim.setguide_right_in,
                R.anim.mappage_gone
            )
        }

        mSettingsDialog.findViewById<View>(R.id.settings_language).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val newLang = if (mLanguage == "zh") "en" else "zh"
            getSharedPreferences("mLanguage", MODE_PRIVATE).edit {
                putString("mLanguage", newLang)
            }
            getSharedPreferences(PREFS_UI_STATE, MODE_PRIVATE).edit {
                putBoolean(KEY_FLASH_HOME, true)
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang))
            MyDialogManager.getInstance().hide(mSettingsDialog)
        }

        musicSwitch.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (mMusicOpened) {
                musicSwitch.setImageResource(R.drawable.icon_music_off)
                mMusicOpened = false
                prefs.edit { putBoolean("music", false) }
                PlayMusic.getInstance().stopBGM()
            } else {
                musicSwitch.setImageResource(R.drawable.icon_music_on)
                mMusicOpened = true
                prefs.edit { putBoolean("music", true) }
                PlayMusic.getInstance().playBGM()
            }
        }

        audioSwitch.setOnClickListener {
            val prefs = getSharedPreferences("music_set", MODE_PRIVATE)
            if (mAudioOpened) {
                audioSwitch.setImageResource(R.drawable.icon_sound_off)
                mAudioOpened = false
                prefs.edit { putBoolean("audio", false) }
            } else {
                audioSwitch.setImageResource(R.drawable.icon_sound_on)
                mAudioOpened = true
                prefs.edit { putBoolean("audio", true) }
                PlayMusic.getInstance().playButtonTap()
            }
        }
    }

    private fun initShootingStar() {
        mBigShootingStar.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shootingstar_big))
        mSmallShootingStar.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shootingstar_small))
        ObjectAnimator.ofFloat(mBackgroundStars, "alpha", 0.5f, 1f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
    }

    private fun initBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mBackPressCount == 1) {
                    finishAffinity()
                } else {
                    Toast.makeText(this@MapActivity, R.string.pressagain, Toast.LENGTH_SHORT).show()
                    mBackPressCount++
                    mHandler.postDelayed({ mBackPressCount = 0 }, 1500)
                }
            }
        })
    }

    private fun getRollingPosition(num: String): Int {
        val position = parseLevel(num) ?: return 1
        val n = (position - 1) / 4
        return if (n in 0..8) 10 - n else 1
    }

    override fun onPause() { super.onPause(); PlayMusic.getInstance().stopBGM() }

    override fun onResume() {
        super.onResume()
        PlayMusic.getInstance().playBGM()
        refreshLoginState()
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        MyDialogManager.getInstance().hide(mSettingsDialog)
    }

    private fun parseLevel(raw: String?): Int? {
        return raw?.toIntOrNull()?.takeIf { it in 1..MAX_LEVEL }
    }

    private fun consumeHomeFlashRequest(): Boolean {
        val fromIntent = intent.getBooleanExtra(EXTRA_FLASH_HOME, false)
        intent.removeExtra(EXTRA_FLASH_HOME)
        val prefs = getSharedPreferences(PREFS_UI_STATE, MODE_PRIVATE)
        val fromPrefs = prefs.getBoolean(KEY_FLASH_HOME, false)
        if (fromPrefs) prefs.edit { putBoolean(KEY_FLASH_HOME, false) }
        return fromIntent || fromPrefs
    }

    private fun flashHome() {
        findViewById<View>(android.R.id.content).post {
            findViewById<View>(android.R.id.content).flashThreeTimes()
        }
    }
}
