package com.bird.starryskysudoku.ui.splash

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.database.DatabaseInitializer
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.guide.GuideActivity
import com.bird.starryskysudoku.ui.map.MapActivity
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_FIRST = "firstcome"
        private const val PREFS_LANGUAGE = "language"
        private const val KEY_FIRST = "first"
        private const val KEY_LANG = "language"
        private const val CHINESE = "zh"
        private const val ENGLISH = "en"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val firstPrefs = getSharedPreferences(PREFS_FIRST, MODE_PRIVATE)
        val langPrefs = getSharedPreferences(PREFS_LANGUAGE, MODE_PRIVATE)
        val isFirst = firstPrefs.getBoolean(KEY_FIRST, true)
        val language = langPrefs.getString(KEY_LANG, CHINESE) ?: CHINESE

        val config = Configuration(resources.configuration)
        if (language == CHINESE) {
            config.setLocale(Locale(CHINESE))
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.splash)
                .setBackgroundResource(R.drawable.sudoku_default_ch)
        } else {
            config.setLocale(Locale(ENGLISH))
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.splash)
                .setBackgroundResource(R.drawable.sudoku_default_eg)
        }
        resources.updateConfiguration(config, resources.displayMetrics)

        ValueAnimator.ofFloat(0.5f, 1f).apply {
            duration = 150
            repeatCount = 6
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.splash)
                    .alpha = it.animatedValue as Float
            }
            start()
        }

        PlayMusic.getInstance().init(application)
        DatabaseInitializer.getDatabase(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (isFirst) {
                startActivity(Intent(this, GuideActivity::class.java))
            } else {
                startActivity(Intent(this, MapActivity::class.java))
            }
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
        }, 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayMusic.getInstance().release()
    }
}
