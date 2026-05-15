package com.bird.starryskysudoku.ui.guide

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.map.MapActivity

class GuideActivity : AppCompatActivity() {

    private lateinit var guides: Array<ConstraintLayout>
    private lateinit var finalGuide: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guidepage)

        PlayMusic.getInstance().playBGM()

        guides = arrayOf(
            findViewById(R.id.guide_1), findViewById(R.id.guide_2),
            findViewById(R.id.guide_3), findViewById(R.id.guide_4),
            findViewById(R.id.guide_5)
        )
        finalGuide = findViewById(R.id.guide_6)
        initTouch()
    }

    private fun initTouch() {
        for (i in 0 until 5) {
            val idx = i
            guides[i].setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                guides[idx].visibility = View.GONE
                if (idx == 4) {
                    finalGuide.visibility = View.VISIBLE
                } else {
                    guides[idx + 1].visibility = View.VISIBLE
                }
            }
        }
        finalGuide.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            getSharedPreferences("firstcome", MODE_PRIVATE)
                .edit().putBoolean("first", false).apply()
            startActivity(Intent(this, MapActivity::class.java))
            overridePendingTransition(R.anim.playpage_show, R.anim.playpage_hide)
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) finishAffinity()
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() { super.onResume(); PlayMusic.getInstance().playBGM() }
    override fun onPause() { super.onPause(); PlayMusic.getInstance().stopBGM() }
}
