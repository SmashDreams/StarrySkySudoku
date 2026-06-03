package com.bird.starryskysudoku.ui.howtoplay

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.bird.starryskysudoku.ui.common.BaseLocalizedActivity
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.databinding.ActivityHowtoplaypageBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.finishWithTransition

class HowToPlayActivity : BaseLocalizedActivity() {
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
        // 玩法说明属于地图页侧边入口，关闭时统一使用返回地图的转场动画。
        PlayMusic.getInstance().playButtonTap()
        finishWithTransition(R.anim.mappage_back, R.anim.setguide_right_out)
    }

}
