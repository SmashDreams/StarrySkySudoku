package com.bird.starryskysudoku.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.ImageView
import com.bird.starryskysudoku.ui.common.BaseLocalizedActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.ui.guide.GuideActivity
import com.bird.starryskysudoku.ui.map.MapRoute
import java.util.Locale

class AppEntryActivity : BaseLocalizedActivity() {

    companion object {
        private const val PREFS_FIRST = "firstcome"
        private const val KEY_FIRST = "first"
        private const val ENGLISH = "en"
        private const val SPLASH_DURATION_MILLIS = 1000L
    }

    private val mHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 启动页直接按当前语言选择整张静态图，避免首屏还要等待布局和文案刷新。
        val splashImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(getSplashImageRes())
        }
        setContentView(
            splashImage,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        mHandler.postDelayed({ openNextPage() }, SPLASH_DURATION_MILLIS)
    }

    override fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun openNextPage() {
        val isFirstLaunch = getSharedPreferences(PREFS_FIRST, MODE_PRIVATE)
            .getBoolean(KEY_FIRST, true)
        // 首次安装先走引导页，之后统一从地图页进入主流程。
        val nextActivity = if (isFirstLaunch) {
            Intent(this, GuideActivity::class.java)
        } else {
            MapRoute.create(this)
        }
        startActivity(nextActivity)
        finish()
    }

    private fun getSplashImageRes(): Int {
        // 优先读取应用当前生效语言，保证启动图和后续页面语言一致。
        val language = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: readSavedLanguage()
            ?: Locale.getDefault().language
        return if (language == ENGLISH) R.drawable.splash_screen_en else R.drawable.splash_screen_zh
    }

    private fun readSavedLanguage(): String? {
        val prefs = getSharedPreferences(AppSettings.PREFS_LANGUAGE, MODE_PRIVATE)
        return if (prefs.contains(AppSettings.KEY_LANGUAGE)) {
            prefs.getString(AppSettings.KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE)
        } else {
            null
        }
    }
}
