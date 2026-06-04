package com.bird.starryskysudoku.ui.map

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.EdgeEffect
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.databinding.ActivityMappageBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.BaseLocalizedActivity
import com.bird.starryskysudoku.ui.play.PlayRoute

class MapActivity : BaseLocalizedActivity() {

    companion object {
        private const val MAX_LEVEL = 40
        private const val MAP_PASS_ROW_HEIGHT_DP = 340
        private const val MAP_BOTTOM_STAR_REVEAL_DP = 96
        private const val MAP_STAR_VISUAL_HEIGHT_DP = 88
        private const val KEY_MAP_RESTORE_POSITION = "map_restore_position"
        private const val KEY_MAP_RESTORE_TOP_OFFSET = "map_restore_top_offset"
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
    private var mPendingRestorePosition: Int? = null
    private var mPendingRestoreTopOffsetPx: Int? = null
    private val mHandler = Handler(Looper.getMainLooper())
    private val mNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            mNotificationNavigator.startPendingPlayPage()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureImmersiveMapWindow()
        mBinding = ActivityMappageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        applyMapSystemBarInsets()

        val db = DatabaseInitializer.getDatabase(this)
        mViewModel = ViewModelProvider(this, MapViewModelFactory(db))[MapViewModel::class.java]

        mPendingRestorePosition = savedInstanceState
            ?.takeIf { it.containsKey(KEY_MAP_RESTORE_POSITION) }
            ?.getInt(KEY_MAP_RESTORE_POSITION)
        mPendingRestoreTopOffsetPx = savedInstanceState
            ?.takeIf { it.containsKey(KEY_MAP_RESTORE_TOP_OFFSET) }
            ?.getInt(KEY_MAP_RESTORE_TOP_OFFSET)

        mRecyclerView = mBinding.passList
        mRecyclerView.visibility = View.INVISIBLE
        mRecyclerView.overScrollMode = View.OVER_SCROLL_ALWAYS
        configureOverscrollLimit(mRecyclerView, 150)
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
        mSettingsController = MapSettingsController(
            mActivity = this,
            mSettingsButton = mSettings,
            mOnLanguageChanged = { refreshVisibleLanguage() }
        ).also { it.init() }
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

    @Suppress("DEPRECATION")
    private fun configureImmersiveMapWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = ContextCompat.getColor(this, R.color.system_bar_night_sky)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    private fun applyMapSystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mBinding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }
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
        mAdapter = PassListAdapter(emptyList(), mPassDialogController.mLightStarsValue)
        mRecyclerView.adapter = mAdapter

        mViewModel.mMapData.observe(this) { data ->
            mAdapter = PassListAdapter(data, mPassDialogController.mLightStarsValue)
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
        if (restorePendingConfigPosition()) return
        if (restorePendingReturnAnchor()) return
        positionMapToLevel(
            level = mPassDialogController.mPendingCompletedLevel,
            skipProgressOffset = mPassDialogController.mHasPendingWinNavigation
        )
    }

    private fun restorePendingConfigPosition(): Boolean {
        // 旋转屏幕或系统回收重建时，优先恢复系统保存的列表位置。
        val adapterPosition = mPendingRestorePosition ?: return false
        val topOffsetPx = mPendingRestoreTopOffsetPx ?: return false
        mPendingRestorePosition = null
        mPendingRestoreTopOffsetPx = null
        if (adapterPosition !in 0 until mAdapter.itemCount) {
            return false
        }
        positionMapAndReveal {
            mLayoutManager.scrollToPositionWithOffset(adapterPosition, topOffsetPx)
        }
        return true
    }

    private fun restorePendingReturnAnchor(): Boolean {
        // 从棋盘页返回地图时，优先回到离开前的关卡附近，而不是重新按当前进度定位。
        if (!mPassDialogController.mHasPendingReturnAnchor) return false
        val adapterPosition = mPassDialogController.mPendingReturnAnchorPosition ?: return false
        val topOffsetPx = mPassDialogController.mPendingReturnAnchorOffsetPx ?: return false
        if (adapterPosition !in 0 until mAdapter.itemCount) return false
        mPassDialogController.clearPendingReturnAnchor()
        positionMapAndReveal {
            mLayoutManager.scrollToPositionWithOffset(adapterPosition, topOffsetPx)
        }
        return true
    }

    private fun positionMapToLevel(level: Int?, skipProgressOffset: Boolean) {
        positionMapAndReveal {
            // 胜利返回时根据已通关关卡做额外上移，其余场景按当前待挑战关卡定位。
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
        mLoginStatus.isEnabled = enabled
        mRecyclerView.alpha = if (enabled) 1f else 0.96f
    }

    private fun positionMapAndReveal(positionAction: () -> Unit) {
        mRecyclerView.post {
            positionAction()
            // 先完成列表定位，再在下一帧显示，避免用户看到滚动到目标位置的过程。
            mRecyclerView.doOnPreDraw {
                mRecyclerView.visibility = View.VISIBLE
                mBinding.root.alpha = 1f
            }
        }
    }


    private fun saveCurrentMapPosition(save: (Int, Int) -> Unit) {
        if (!::mLayoutManager.isInitialized || !::mRecyclerView.isInitialized) return
        val position = mLayoutManager.findFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return
        // 保存首个可见项和顶部偏移，保证返回后视觉位置与离开时一致。
        save(position, mRecyclerView.getChildAt(0)?.top ?: 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveCurrentMapPosition { position, topOffset ->
            outState.putInt(KEY_MAP_RESTORE_POSITION, position)
            outState.putInt(KEY_MAP_RESTORE_TOP_OFFSET, topOffset)
        }
        super.onSaveInstanceState(outState)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun refreshVisibleLanguage() {
        refreshLoginState()
        if (::mAdapter.isInitialized) mAdapter.notifyItemChanged(0)
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
        // 打开棋盘页前把地图锚点写入 Intent，供失败/退出后恢复原浏览位置。
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

    private fun configureOverscrollLimit(recyclerView: RecyclerView, limitDp: Int) {
        val maxPx = dpToPx(limitDp).toFloat()
        recyclerView.edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
            override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
                return object : EdgeEffect(view.context) {
                    override fun onPull(deltaDistance: Float) {
                        super.onPull(deltaDistance.coerceIn(-maxPx, maxPx))
                    }
                    override fun onPull(deltaDistance: Float, displacement: Float) {
                        super.onPull(deltaDistance.coerceIn(-maxPx, maxPx), displacement)
                    }
                }
            }
        }
    }
}
