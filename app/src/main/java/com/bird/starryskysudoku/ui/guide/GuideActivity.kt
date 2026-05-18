package com.bird.starryskysudoku.ui.guide

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.map.MapActivity

class GuideActivity : AppCompatActivity() {

    private lateinit var mGuides: Array<ConstraintLayout>
    private lateinit var mFinalGuide: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guidepage)

        PlayMusic.getInstance().playBGM()

        mGuides = arrayOf(
            findViewById(R.id.guide_1), findViewById(R.id.guide_2),
            findViewById(R.id.guide_3), findViewById(R.id.guide_4),
            findViewById(R.id.guide_5)
        )
        mFinalGuide = findViewById(R.id.guide_6)
        initTouch()
        initBackHandler()
    }

    private fun initTouch() {
        for (i in 0 until 5) {
            val idx = i
            mGuides[i].setOnClickListener {
                PlayMusic.getInstance().playButtonTap()
                mGuides[idx].visibility = View.GONE
                if (idx == 4) {
                    mFinalGuide.visibility = View.VISIBLE
                } else {
                    mGuides[idx + 1].visibility = View.VISIBLE
                }
            }
        }
        mFinalGuide.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            getSharedPreferences("firstcome", MODE_PRIVATE).edit {
                putBoolean("first", false)
            }
            startActivityWithTransition(
                Intent(this, MapActivity::class.java)
                    .putExtra(MapActivity.EXTRA_FLASH_HOME, false),
                R.anim.playpage_show,
                R.anim.playpage_hide
            )
            finish()
        }
    }

    private fun initBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    override fun onResume() { super.onResume(); PlayMusic.getInstance().playBGM() }
    override fun onPause() { super.onPause(); PlayMusic.getInstance().stopBGM() }
}
