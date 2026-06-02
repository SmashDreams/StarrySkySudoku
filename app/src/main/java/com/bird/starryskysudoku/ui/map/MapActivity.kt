package com.bird.starryskysudoku.ui.map

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.databinding.ActivityMappageBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.play.PlayRoute

class MapActivity : AppCompatActivity() {

    companion object {
        private const val MAX_LEVEL = 40
        private const val MAP_PASS_ROW_HEIGHT_DP = 340
        private const val MAP_BOTTOM_STAR_REVEAL_DP = 96
        private const val MAP_STAR_VISUAL_HEIGHT_DP = 88
    }

    private lateinit var mSettings: ImageView
    private lateinit var mBinding: ActivityMappageBinding
    private lateinit var mBigShootingStar: ImageView
    private lateinit var mSmallShootingStar: ImageView
    private lateinit var mBackgroundStars: ImageView
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mLayoutManager: LinearLayoutManager
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
            mScrollAfterCompletedLevel = { completedLevel ->
                scrollMapAfterCompletedLevel(completedLevel)
            },
            mSetMapInteractionEnabled = { enabled ->
                setMapInteractionEnabled(enabled)
            }
        )

        initList()
        initShootingStar()
        refreshLoginState()
        initMapData()
        initBackHandler()
    }

    private fun initMapData() {
        mMapLoaded = true
        mPassDialogController.consumeNavigationExtras(intent)
        mViewModel.loadMapData(mCurrentUsername)
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
        mLayoutManager = LinearLayoutManager(this@MapActivity)
        mRecyclerView.layoutManager = mLayoutManager
        mAdapter = PassListAdapter(emptyList(), mPassDialogController.lightStars)
        mRecyclerView.adapter = mAdapter

        mViewModel.mMapData.observe(this) { data ->
            mAdapter = PassListAdapter(data, mPassDialogController.lightStars)
            mRecyclerView.adapter = mAdapter
            positionMapToCurrentPass()
            mAdapter.setOpenListener(object : PassListAdapter.OpenPlayPage {
                override fun onOpen(entity: MapEntity) {
                    mPassDialogController.openForEntity(entity)
                }
            })
        }
    }


    private fun positionMapToCurrentPass() {
        if (restorePendingReturnAnchor()) return
        positionMapToLevel(
            level = mPassDialogController.pendingCompletedLevel,
            skipProgressOffset = mPassDialogController.hasPendingWinNavigation
        )
    }

    private fun restorePendingReturnAnchor(): Boolean {
        if (!mPassDialogController.hasPendingReturnAnchor) return false
        val adapterPosition = mPassDialogController.pendingReturnAnchorPosition ?: return false
        val topOffsetPx = mPassDialogController.pendingReturnAnchorOffsetPx ?: return false
        if (adapterPosition !in 0 until mAdapter.itemCount) return false
        mPassDialogController.clearPendingReturnAnchor()
        mRecyclerView.post {
            mLayoutManager.scrollToPositionWithOffset(adapterPosition, topOffsetPx)
        }
        return true
    }

    private fun positionMapToLevel(level: Int?, skipProgressOffset: Boolean) {
        mRecyclerView.post {
            val completedOffsetDp = if (skipProgressOffset) {
                0
            } else {
                mAdapter.getCurrentProgressOffsetDp()
            }
            val targetLevel = level ?: mAdapter.getCurrentTodoLevel()
            val adapterPosition = targetLevel?.let { mAdapter.getPositionForLevel(it) } ?: mAdapter.getPosition()
            val levelOffsetDp = targetLevel?.let { mAdapter.getTopOffsetDpForLevel(it) } ?: 0
            val offset = mRecyclerView.height -
                dpToPx(MAP_BOTTOM_STAR_REVEAL_DP + MAP_STAR_VISUAL_HEIGHT_DP) -
                dpToPx(levelOffsetDp) +
                dpToPx(completedOffsetDp)
            mLayoutManager.scrollToPositionWithOffset(adapterPosition, offset)
        }
    }

    private fun scrollMapAfterCompletedLevel(completedLevel: Int) {
        val offsetDp = MapScrollPolicy.offsetDpAfterCompletedLevel(completedLevel)
        if (offsetDp <= 0) return
        mRecyclerView.smoothScrollBy(0, -dpToPx(offsetDp))
    }

    private fun setMapInteractionEnabled(enabled: Boolean) {
        mRecyclerView.isEnabled = enabled
        mSettings.isEnabled = enabled
        mRecyclerView.alpha = if (enabled) 1f else 0.96f
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun initShootingStar() {
        startShootingStarLoop(
            star = mBigShootingStar,
            delayMillis = 0L,
            durationMillis = 1200L,
            translateX = -1300f,
            translateY = 550f
        )
        startShootingStarLoop(
            star = mSmallShootingStar,
            delayMillis = 2000L,
            durationMillis = 600L,
            translateX = -1100f,
            translateY = 400f
        )
        ObjectAnimator.ofFloat(mBackgroundStars, "alpha", 0.5f, 1f).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun startShootingStarLoop(
        star: ImageView,
        delayMillis: Long,
        durationMillis: Long,
        translateX: Float,
        translateY: Float
    ) {
        val runnable = object : Runnable {
            override fun run() {
                star.alpha = 0f
                star.translationX = 0f
                star.translationY = 0f
                val moveX = ObjectAnimator.ofFloat(star, "translationX", 0f, translateX)
                val moveY = ObjectAnimator.ofFloat(star, "translationY", 0f, translateY)
                val fadeIn = ObjectAnimator.ofFloat(star, "alpha", 0f, 1f).setDuration(200L)
                val fadeOut = ObjectAnimator.ofFloat(star, "alpha", 1f, 0f).apply {
                    startDelay = (durationMillis - 200L).coerceAtLeast(0L)
                    duration = 200L
                }
                AnimatorSet().apply {
                    playTogether(moveX, moveY, fadeIn, fadeOut)
                    duration = durationMillis
                    interpolator = LinearInterpolator()
                    start()
                }
                mHandler.postDelayed(this, 6000L)
            }
        }
        mHandler.postDelayed(runnable, delayMillis)
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
        val intent = PlayRoute.create(this@MapActivity, parseLevel(num) ?: 1, mCurrentUsername)
        return MapRoute.putReturnAnchor(
            intent = intent,
            adapterPosition = mLayoutManager.findFirstVisibleItemPosition(),
            topOffsetPx = mRecyclerView.getChildAt(0)?.top ?: 0
        )
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
}
