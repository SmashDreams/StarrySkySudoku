package com.bird.starryskysudoku.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.ui.common.flashThreeTimes
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.guide.GuideActivity
import com.bird.starryskysudoku.ui.map.MapActivity

class AppEntryActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_FIRST = "firstcome"
        private const val KEY_FIRST = "first"
        private const val ENGLISH = "en"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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

        val isFirstLaunch = getSharedPreferences(PREFS_FIRST, MODE_PRIVATE)
            .getBoolean(KEY_FIRST, true)
        val nextActivity = if (isFirstLaunch) {
            Intent(this, GuideActivity::class.java)
        } else {
            Intent(this, MapActivity::class.java).putExtra(MapActivity.EXTRA_FLASH_HOME, false)
        }
        splashImage.flashThreeTimes {
            startActivityWithTransition(nextActivity, R.anim.playpage_show, R.anim.playpage_hide)
            finish()
        }
    }

    private fun getSplashImageRes(): Int {
        val language = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: getSharedPreferences(AppSettings.PREFS_LANGUAGE, MODE_PRIVATE)
                .getString(AppSettings.KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE)
        return if (language == ENGLISH) R.drawable.splash_screen_en else R.drawable.splash_screen_zh
    }
}
