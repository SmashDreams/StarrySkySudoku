package com.bird.starryskysudoku.ui.map

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.databinding.ActivityMappageBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.flashThreeTimes
import com.bird.starryskysudoku.ui.play.PlayRoute

class MapActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LEVEL = 40
        const val EXTRA_FLASH_HOME = MapRoute.EXTRA_FLASH_HOME
        private const val PREFS_UI_STATE = "ui_state"
        private const val KEY_FLASH_HOME = "flash_home"
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
    private lateinit var mPassDialogController: MapPassDialogController
    private lateinit var mSettingsController: MapSettingsController
    private lateinit var mNotificationNavigator: MapNotificationNavigator

    private var mBackPressCount = 0
    private var mCurrentUsername = LauncherSessionReader.GUEST_USERNAME
    private var mMapLoaded = false
    private val mHandler = Handler(Looper.getMainLooper())
    private val mNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            mNotificationNavigator.startPendingPlayPage()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMappageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        val db = DatabaseInitializer.getDatabase(this)
        mViewModel = ViewModelProvider(this, MapViewModelFactory(db))[MapViewModel::class.java]

        mRecyclerView = mBinding.passList
        mBigShootingStar = mBinding.sstarBig
        mSmallShootingStar = mBinding.sstarSmall
        mBackgroundStars = mBinding.mapBgstar
        mSettings = mBinding.settings
        mLoginStatus = mBinding.loginStatus
        mLoginStatus.setOnClickListener {
            Toast.makeText(this, R.string.login_prompt_teahouse, Toast.LENGTH_SHORT).show()
        }

        /*
         * 地图页只负责组装控制器，弹窗、设置和通知权限流程分别放到独立类里维护。
         */
        mNotificationNavigator = MapNotificationNavigator(this, mNotificationPermissionLauncher)
        mSettingsController = MapSettingsController(this, mSettings).also { it.init() }
        mPassDialogController = MapPassDialogController(
            mActivity = this,
            mLayoutInflater = layoutInflater,
            mRecyclerView = mRecyclerView,
            mViewModel = mViewModel,
            mHandler = mHandler,
            mGetUsername = { mCurrentUsername },
            mCreatePlayIntent = { createPlayIntent(it) },
            mOpenPlayPage = { playIntent, finishMapAfterStart ->
                mNotificationNavigator.openPlayPageAfterNotificationPermission(
                    playIntent,
                    finishMapAfterStart
                )
            },
            mParseLevel = { parseLevel(it) },
            mGetRollingPosition = { getRollingPosition(it) }
        )

        initList()
        initShootingStar()
        refreshLoginState()
        initMapData()
        initBackHandler()
        if (consumeHomeFlashRequest()) flashHome()
    }

    private fun initMapData() {
        mMapLoaded = true
        mViewModel.loadMapData(mCurrentUsername)
        mPassDialogController.consumeNavigationExtras(intent)
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

    private fun initList() {
        mRecyclerView.layoutManager = LinearLayoutManager(this@MapActivity)
        mAdapter = PassListAdapter(emptyList(), mPassDialogController.lightStars)
        mRecyclerView.adapter = mAdapter

        mViewModel.mMapData.observe(this) { data ->
            mAdapter = PassListAdapter(data, mPassDialogController.lightStars)
            mRecyclerView.adapter = mAdapter
            mRecyclerView.scrollToPosition(mAdapter.getPosition())
            mRecyclerView.smoothScrollBy(0, 150)

            mAdapter.setOpenListener(object : PassListAdapter.OpenPlayPage {
                override fun onOpen(entity: MapEntity) {
                    mPassDialogController.openForEntity(entity)
                }
            })
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

    private fun createPlayIntent(num: String): Intent {
        return PlayRoute.create(this@MapActivity, parseLevel(num) ?: 1, mCurrentUsername)
    }

    private fun getRollingPosition(num: String): Int {
        val position = parseLevel(num) ?: return 1
        val n = (position - 1) / 4
        return if (n in 0..8) 10 - n else 1
    }

    override fun onPause() {
        mNotificationNavigator.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        mNotificationNavigator.onResume()
        refreshLoginState()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        mNotificationNavigator.onWindowFocusChanged(hasFocus)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        mSettingsController.hide()
        mNotificationNavigator.onDestroy()
    }

    private fun parseLevel(raw: String?): Int? {
        return raw?.toIntOrNull()?.takeIf { it in 1..MAX_LEVEL }
    }

    private fun consumeHomeFlashRequest(): Boolean {
        val prefs = getSharedPreferences(PREFS_UI_STATE, Context.MODE_PRIVATE)
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
