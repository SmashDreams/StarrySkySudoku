package com.bird.starryskysudoku.ui.howtoplay

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.databinding.ActivityHowtoplaypageBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.finishWithTransition

class HowToPlayActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityHowtoplaypageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityHowtoplaypageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        mBinding.guideBack.setOnClickListener {
            closePage()
        }
        initBackHandler()
    }

    private fun initBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closePage()
            }
        })
    }

    private fun closePage() {
        PlayMusic.getInstance().playButtonTap()
        finishWithTransition(R.anim.mappage_back, R.anim.setguide_right_out)
    }

    override fun onResume() { super.onResume(); PlayMusic.getInstance().playBGM() }
    override fun onPause() { super.onPause(); PlayMusic.getInstance().stopBGM() }
}
