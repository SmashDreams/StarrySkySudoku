package com.bird.starryskysudoku.ui.map

import android.content.Intent
import android.os.Handler
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.repository.PassStatus
import com.bird.starryskysudoku.databinding.DialogPasscheckBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.dialog.MyDialogManager

class MapPassDialogController(
    private val mActivity: AppCompatActivity,
    private val mLayoutInflater: LayoutInflater,
    private val mRecyclerView: RecyclerView,
    private val mViewModel: MapViewModel,
    private val mHandler: Handler,
    private val mGetUsername: () -> String,
    private val mCreatePlayIntent: (String) -> Intent,
    private val mOpenPlayPage: (Intent, Boolean) -> Unit,
    private val mParseLevel: (String?) -> Int?,
    private val mGetRollingPosition: (String) -> Int
) {
    private var mNextNum: String? = null
    private var mLoseNum: String? = null
    private var mDelayTime = 0
    private var mLightStars = 0

    val lightStars: Int
        get() = mLightStars

    fun consumeNavigationExtras(intent: Intent) {
        /*
         * 胜利/失败返回地图时会携带关卡信息，这里统一消费并清理，避免页面恢复后重复弹窗。
         */
        mNextNum = null
        mLoseNum = null
        intent.getStringExtra(MapRoute.EXTRA_ROLL_LEVEL)
            ?.let { mRecyclerView.scrollToPosition(mGetRollingPosition(it)) }
        mNextNum = intent.getStringExtra(MapRoute.EXTRA_NEXT_LEVEL)?.takeIf { mParseLevel(it) != null }
        mLoseNum = intent.getStringExtra(MapRoute.EXTRA_LOSE_LEVEL)?.takeIf { mParseLevel(it) != null }
        intent.removeExtra(MapRoute.EXTRA_ROLL_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_NEXT_LEVEL)
        intent.removeExtra(MapRoute.EXTRA_LOSE_LEVEL)

        mNextNum?.let { mDelayTime = 1050; handleCheckNum(it) }
        mLoseNum?.let {
            mDelayTime = 500
            handleCheckNum(it)
            if (mNextNum == null) mRecyclerView.scrollToPosition(mGetRollingPosition(it))
        }
    }

    fun openForEntity(entity: MapEntity) {
        when (entity.mStatus) {
            PassStatus.TODO -> openPassCheck(entity.mPassNum.toString())
            PassStatus.COMPLETED -> openRetryCheck(entity.mPassNum.toString())
        }
    }

    private fun handleCheckNum(num: String) {
        val passNum = mParseLevel(num) ?: return
        mViewModel.getPassStatus(mGetUsername(), passNum) { status ->
            when (status) {
                PassStatus.TODO -> openPassCheck(passNum.toString())
                PassStatus.COMPLETED -> openRetryCheck(passNum.toString())
            }
        }
    }

    private fun openPassCheck(checkNum: String) {
        if (mNextNum != null) {
            /*
             * 新通关星星的点亮动画需要先更新列表高亮数，再延迟展示下一关弹窗。
             */
            mLightStars = mNextNum!!.toInt() - 1
            mHandler.postDelayed({
                if (mNextNum!!.toInt() % 4 == 0) {
                    mRecyclerView.smoothScrollBy(0, -400)
                    mDelayTime = 1550
                }
            }, 700)
        }

        val passCheckBinding = DialogPasscheckBinding.inflate(mLayoutInflater)
        val dialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_passcheck, passCheckBinding.root)
            .apply {
                passCheckBinding.passcheckNum.text = checkNum
                passCheckBinding.passcheckPasstimes.text = "0"
                passCheckBinding.passcheckStar.setImageResource(R.drawable.star_empty)

                passCheckBinding.passcheckClose.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        MyDialogManager.getInstance().hide(this)
                    }, 200)
                }

                passCheckBinding.passcheckStart.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        MyDialogManager.getInstance().hide(this)
                        mOpenPlayPage(mCreatePlayIntent(checkNum), true)
                    }, 165)
                }
            }

        mHandler.postDelayed({
            MyDialogManager.getInstance().show(dialog)
        }, mDelayTime.toLong())
    }

    private fun openRetryCheck(checkNum: String) {
        val passCheckBinding = DialogPasscheckBinding.inflate(mLayoutInflater)
        val dialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_passcheck, passCheckBinding.root)
            .apply {
                passCheckBinding.passcheckStar.setImageResource(R.drawable.star_empty)
                passCheckBinding.passcheckNum.text = checkNum

                mViewModel.getPassTimes(mGetUsername(), checkNum.toInt()) { times ->
                    passCheckBinding.passcheckPasstimes.text = times
                }

                passCheckBinding.passcheckClose.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        MyDialogManager.getInstance().hide(this)
                    }, 200)
                }

                passCheckBinding.passcheckStart.setOnClickListener {
                    PlayMusic.getInstance().playButtonTap()
                    mHandler.postDelayed({
                        MyDialogManager.getInstance().hide(this)
                        mOpenPlayPage(mCreatePlayIntent(checkNum), false)
                    }, 165)
                }
            }

        mHandler.postDelayed({
            MyDialogManager.getInstance().show(dialog)
        }, mDelayTime.toLong())
    }
}
