package com.bird.starryskysudoku.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.repository.PassStatus
import com.bird.starryskysudoku.databinding.ActivityMappageBinding
import com.bird.starryskysudoku.databinding.DialogPasscheckBinding
import com.bird.starryskysudoku.databinding.DialogSettingsBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.notification.NotificationPermissionPolicy
import com.bird.starryskysudoku.timer.CountdownTimerContract
import com.bird.starryskysudoku.ui.common.flashThreeTimes
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.ui.howtoplay.HowToPlayActivity
import com.bird.starryskysudoku.ui.play.PlayRoute

class MapActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LEVEL = 40
        const val EXTRA_FLASH_HOME = MapRoute.EXTRA_FLASH_HOME
        private const val PREFS_UI_STATE = "ui_state"
        private const val KEY_FLASH_HOME = "flash_home"
        private const val PREFS_NOTIFICATION_STATE = "notification_state"
        private const val KEY_VENDOR_WARMUP_ATTEMPTED = "vendor_warmup_attempted"
        private const val VENDOR_WARMUP_NOTIFICATION_ID = 1002
        private const val VENDOR_WARMUP_DELAY_MS = 700L
    }

    private lateinit var mSettings: ImageView
    private lateinit var mBinding: ActivityMappageBinding
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
    private val mNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startPendingPlayPage()
        }
    private var mPendingPlayIntent: Intent? = null
    private var mPendingPlayShouldFinishMap = false
    private var mWaitingVendorNotificationWarmup = false
    private var mCanCompleteVendorNotificationWarmup = false
    private var mMapResumed = false
    private var mMapHasWindowFocus = false

    private lateinit var mSettingsDialog: MyDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMappageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val db = DatabaseInitializer.getDatabase(this)
        mViewModel = ViewModelProvider(this, MapViewModelFactory(db))[MapViewModel::class.java]

        val musicPrefs = getSharedPreferences(AppSettings.PREFS_MUSIC, MODE_PRIVATE)
        mMusicOpened = musicPrefs.getBoolean(AppSettings.KEY_MUSIC, true)
        mAudioOpened = musicPrefs.getBoolean(AppSettings.KEY_AUDIO, true)
        mLanguage = getSharedPreferences(AppSettings.PREFS_LANGUAGE, MODE_PRIVATE)
            .getString(AppSettings.KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE) ?: AppSettings.DEFAULT_LANGUAGE

        mRecyclerView = mBinding.passList
        mBigShootingStar = mBinding.sstarBig
        mSmallShootingStar = mBinding.sstarSmall
        mBackgroundStars = mBinding.mapBgstar
        mSettings = mBinding.settings
        mLoginStatus = mBinding.loginStatus
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
        mViewModel.loadMapData(mCurrentUsername)
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
        mNextNum = null
        mLoseNum = null
        intent.getStringExtra(MapRoute.EXTRA_ROLL_LEVEL)
            ?.let { mRecyclerView.scrollToPosition(getRollingPosition(it)) }
        mNextNum = intent.getStringExtra(MapRoute.EXTRA_NEXT_LEVEL)?.takeIf { parseLevel(it) != null }
        mLoseNum = intent.getStringExtra(MapRoute.EXTRA_LOSE_LEVEL)?.takeIf { parseLevel(it) != null }
        intent.removeExtra(MapRoute.EXTRA_ROLL_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_NEXT_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_LOSE_LEVEL)

        mNextNum?.let { mDelayTime = 1050; handleCheckNum(it) }
        mLoseNum?.let {
            mDelayTime = 500
            handleCheckNum(it)
            if (mNextNum == null) mRecyclerView.scrollToPosition(getRollingPosition(it))
        }
    }

    private fun handleCheckNum(num: String) {
        val passNum = parseLevel(num) ?: return
        mViewModel.getPassStatus(mCurrentUsername, passNum) { status ->
            when (status) {
                PassStatus.TODO -> openPassCheck(passNum.toString())
                PassStatus.COMPLETED -> openRetryCheck(passNum.toString())
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

        val passCheckBinding = DialogPasscheckBinding.inflate(layoutInflater)
        val dialog = MyDialogManager.getInstance()
            .initView(this, R.layout.dialog_passcheck, passCheckBinding.root)
            .apply {
                passCheckBinding.passcheckNum.text = checkNum
                passCheckBinding.passcheckPasstimes.text = "0"
                passCheckBinding.passcheckStar.setImageResource(R.drawable.star_empty)

                passCheckBinding.passcheckClose.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        MyDialogManager.getInstance().hide(this)
                    }, 200)
                }

                passCheckBinding.passcheckStart.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        MyDialogManager.getInstance().hide(this)
                        openPlayPageAfterNotificationPermission(
                            createPlayIntent(checkNum),
                            finishMapAfterStart = true
                        )
                    }, 165)
                }
            }

        mHandler.postDelayed({
            MyDialogManager.getInstance().show(dialog)
        }, mDelayTime.toLong())
    }

    private fun openRetryCheck(checkNum: String) {
        val passCheckBinding = DialogPasscheckBinding.inflate(layoutInflater)
        val dialog = MyDialogManager.getInstance()
            .initView(this, R.layout.dialog_passcheck, passCheckBinding.root)
            .apply {
                passCheckBinding.passcheckStar.setImageResource(R.drawable.star_empty)
                passCheckBinding.passcheckNum.text = checkNum

                mViewModel.getPassTimes(mCurrentUsername, checkNum.toInt()) { times ->
                    passCheckBinding.passcheckPasstimes.text = times
                }

                passCheckBinding.passcheckClose.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        MyDialogManager.getInstance().hide(this)
                    }, 200)
                }

                passCheckBinding.passcheckStart.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        intent.removeExtra(MapRoute.EXTRA_NEXT_LEVEL)
                        intent.removeExtra(MapRoute.EXTRA_LOSE_LEVEL)
                        MyDialogManager.getInstance().hide(this)
                        openPlayPageAfterNotificationPermission(
                            createPlayIntent(checkNum),
                            finishMapAfterStart = false
                        )
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
                override fun onOpen(entity: MapEntity) {
                    when (entity.mStatus) {
                        PassStatus.TODO -> openPassCheck(entity.mPassNum.toString())
                        PassStatus.COMPLETED -> openRetryCheck(entity.mPassNum.toString())
                    }
                }
            })
        }
    }

    private fun initSettingDialog() {
        val settingsBinding = DialogSettingsBinding.inflate(layoutInflater)
        mSettingsDialog = MyDialogManager.getInstance()
            .initView(this, R.layout.dialog_settings, settingsBinding.root)
        mSettingsDialog.setCanceledOnTouchOutside(true)

        val musicSwitch = settingsBinding.settingsMusic
        val audioSwitch = settingsBinding.settingsAudio

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

        settingsBinding.settingsClose.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mSettingsDialog)
        }

        settingsBinding.settingsHowtoplay.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            startActivityWithTransition(
                Intent(this, HowToPlayActivity::class.java),
                R.anim.setguide_right_in,
                R.anim.mappage_gone
            )
        }

        settingsBinding.settingsLanguage.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val newLang = if (mLanguage == "zh") "en" else "zh"
            mLanguage = newLang
            getSharedPreferences(AppSettings.PREFS_LANGUAGE, MODE_PRIVATE).edit {
                putString(AppSettings.KEY_LANGUAGE, newLang)
            }
            getSharedPreferences(PREFS_UI_STATE, MODE_PRIVATE).edit {
                putBoolean(KEY_FLASH_HOME, true)
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang))
            MyDialogManager.getInstance().hide(mSettingsDialog)
        }

        musicSwitch.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val prefs = getSharedPreferences(AppSettings.PREFS_MUSIC, MODE_PRIVATE)
            if (mMusicOpened) {
                musicSwitch.setImageResource(R.drawable.icon_music_off)
                mMusicOpened = false
                prefs.edit { putBoolean(AppSettings.KEY_MUSIC, false) }
                PlayMusic.getInstance().stopBGM()
            } else {
                musicSwitch.setImageResource(R.drawable.icon_music_on)
                mMusicOpened = true
                prefs.edit { putBoolean(AppSettings.KEY_MUSIC, true) }
                PlayMusic.getInstance().playBGM()
            }
        }

        audioSwitch.setOnClickListener {
            val prefs = getSharedPreferences(AppSettings.PREFS_MUSIC, MODE_PRIVATE)
            if (mAudioOpened) {
                audioSwitch.setImageResource(R.drawable.icon_sound_off)
                mAudioOpened = false
                prefs.edit { putBoolean(AppSettings.KEY_AUDIO, false) }
            } else {
                audioSwitch.setImageResource(R.drawable.icon_sound_on)
                mAudioOpened = true
                prefs.edit { putBoolean(AppSettings.KEY_AUDIO, true) }
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

    private fun openPlayPageAfterNotificationPermission(playIntent: Intent, finishMapAfterStart: Boolean) {
        val permissionStatus = getPostNotificationsStatus()
        if (
            NotificationPermissionPolicy.shouldWarmUpVendorNotificationsBeforePlay(
                sdkInt = Build.VERSION.SDK_INT,
                manufacturer = Build.MANUFACTURER,
                hasWarmupAttempted = hasVendorNotificationWarmupAttempted()
            )
        ) {
            warmUpVendorNotificationsBeforePlay(playIntent, finishMapAfterStart)
            return
        }

        if (
            NotificationPermissionPolicy.shouldStartPlayImmediately(
                sdkInt = Build.VERSION.SDK_INT,
                permissionStatus = permissionStatus
            )
        ) {
            startPlayPage(playIntent, finishMapAfterStart)
            return
        }

        mPendingPlayIntent = playIntent
        mPendingPlayShouldFinishMap = finishMapAfterStart
        mNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun warmUpVendorNotificationsBeforePlay(playIntent: Intent, finishMapAfterStart: Boolean) {
        mPendingPlayIntent = playIntent
        mPendingPlayShouldFinishMap = finishMapAfterStart
        mWaitingVendorNotificationWarmup = true
        mCanCompleteVendorNotificationWarmup = false
        showVendorNotificationPreflight()
        mHandler.postDelayed({
            mCanCompleteVendorNotificationWarmup = true
            completeVendorNotificationWarmupIfReady()
        }, VENDOR_WARMUP_DELAY_MS)
    }

    private fun completeVendorNotificationWarmupIfReady() {
        if (!mWaitingVendorNotificationWarmup) return
        if (!mCanCompleteVendorNotificationWarmup || !mMapResumed || !mMapHasWindowFocus) return

        markVendorNotificationWarmupAttempted()
        NotificationManagerCompat.from(this).cancel(VENDOR_WARMUP_NOTIFICATION_ID)
        mWaitingVendorNotificationWarmup = false
        mCanCompleteVendorNotificationWarmup = false
        startPendingPlayPage()
    }

    @SuppressLint("MissingPermission")
    private fun showVendorNotificationPreflight() {
        createNotificationChannel()
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MapActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CountdownTimerContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(getString(R.string.notification_preflight_title))
            .setContentText(getString(R.string.notification_preflight_text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(VENDOR_WARMUP_NOTIFICATION_ID, notification)
        } catch (exception: SecurityException) {
            /*
             * 厂商系统可能直接拦截通知；这里不阻塞进入棋盘，只把权限触发尽量提前到地图页。
             */
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CountdownTimerContract.NOTIFICATION_CHANNEL_ID,
            getString(R.string.countdown_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            description = getString(R.string.countdown_notification_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startPendingPlayPage() {
        val playIntent = mPendingPlayIntent ?: return
        val finishMapAfterStart = mPendingPlayShouldFinishMap
        mPendingPlayIntent = null
        mPendingPlayShouldFinishMap = false
        startPlayPage(playIntent, finishMapAfterStart)
    }

    private fun startPlayPage(playIntent: Intent, finishMapAfterStart: Boolean) {
        startActivityWithTransition(
            playIntent,
            R.anim.playpage_show,
            R.anim.playpage_hide
        )
        if (finishMapAfterStart) finish()
    }

    private fun createPlayIntent(num: String): Intent {
        return PlayRoute.create(this@MapActivity, parseLevel(num) ?: 1, mCurrentUsername)
    }

    private fun getPostNotificationsStatus(): Int {
        val permissionStatus = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return permissionStatus
    }

    private fun hasVendorNotificationWarmupAttempted(): Boolean {
        return getSharedPreferences(PREFS_NOTIFICATION_STATE, MODE_PRIVATE)
            .getBoolean(KEY_VENDOR_WARMUP_ATTEMPTED, false)
    }

    private fun markVendorNotificationWarmupAttempted() {
        getSharedPreferences(PREFS_NOTIFICATION_STATE, MODE_PRIVATE).edit {
            putBoolean(KEY_VENDOR_WARMUP_ATTEMPTED, true)
        }
    }

    private fun getRollingPosition(num: String): Int {
        val position = parseLevel(num) ?: return 1
        val n = (position - 1) / 4
        return if (n in 0..8) 10 - n else 1
    }

    override fun onPause() {
        mMapResumed = false
        super.onPause()
        PlayMusic.getInstance().stopBGM()
    }

    override fun onResume() {
        super.onResume()
        mMapResumed = true
        PlayMusic.getInstance().playBGM()
        refreshLoginState()
        completeVendorNotificationWarmupIfReady()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        mMapHasWindowFocus = hasFocus
        if (hasFocus) completeVendorNotificationWarmupIfReady()
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
        val prefs = getSharedPreferences(PREFS_UI_STATE, MODE_PRIVATE)
        val fromPrefs = prefs.getBoolean(KEY_FLASH_HOME, false)
        if (fromPrefs) prefs.edit { putBoolean(KEY_FLASH_HOME, false) }
        return MapRoute.consumeHomeFlashRequest(intent, fromPrefs)
    }

    private fun flashHome() {
        mBinding.root.post {
            mBinding.root.flashThreeTimes()
        }
    }
}
