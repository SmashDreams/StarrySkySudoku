package com.bird.starryskysudoku.ui.howtoplay

import android.os.Bundle
import android.view.KeyEvent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.media.PlayMusic

class HowToPlayActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_howtoplaypage)
        findViewById<ImageView>(R.id.guide_back).setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            finish()
            overridePendingTransition(R.anim.mappage_back, R.anim.setguide_right_out)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            PlayMusic.getInstance().playButtonTap()
            finish()
            overridePendingTransition(R.anim.mappage_back, R.anim.setguide_right_out)
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() { super.onResume(); PlayMusic.getInstance().playBGM() }
    override fun onPause() { super.onPause(); PlayMusic.getInstance().stopBGM() }
}
