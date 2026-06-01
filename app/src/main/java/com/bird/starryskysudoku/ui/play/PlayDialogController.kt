package com.bird.starryskysudoku.ui.play

import android.animation.ObjectAnimator
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.databinding.DialogLoseBinding
import com.bird.starryskysudoku.databinding.DialogPauseBinding
import com.bird.starryskysudoku.databinding.DialogWinBinding
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.ui.map.MapRoute

class PlayDialogController(
    private val mActivity: AppCompatActivity,
    private val mMaxLevel: Int,
    private val mGetLevel: () -> Int,
    private val mGetUsername: () -> String,
    private val mRunAfterClearingHistory: (() -> Unit) -> Unit,
    private val mSetPaused: (Boolean) -> Unit,
    private val mStartCountdownService: () -> Unit,
    private val mPrepareForReplacementPlayActivity: () -> Unit
) {
    private var mMusicOpened = true
    private var mAudioOpened = true
    private lateinit var mPauseDialog: MyDialog
    private lateinit var mWinDialog: MyDialog
    private lateinit var mLoseDialog: MyDialog
    private lateinit var mWinDialogBinding: DialogWinBinding

    init {
        val musicPrefs = mActivity.getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
        mMusicOpened = musicPrefs.getBoolean(AppSettings.KEY_MUSIC, true)
        mAudioOpened = musicPrefs.getBoolean(AppSettings.KEY_AUDIO, true)
        initPauseDialog()
        initLoseDialog()
        initWinDialog()
    }

    fun showPauseDialog() {
        MyDialogManager.getInstance().show(mPauseDialog)
    }

    fun showLoseDialog() {
        MyDialogManager.getInstance().show(mLoseDialog)
    }

    fun showWinDialog() {
        MyDialogManager.getInstance().show(mWinDialog)
    }

    fun showWinDialogWithStarAnimation() {
        showWinDialog()
        PlayMusic.getInstance().playGetStar()
        val winStar = mWinDialogBinding.winStarOn
        ObjectAnimator.ofFloat(winStar, "alpha", 0f, 1f).setDuration(500).start()
        ObjectAnimator.ofFloat(winStar, "scaleX", 0.5f, 1.1f, 1f).setDuration(500).start()
        ObjectAnimator.ofFloat(winStar, "scaleY", 0.5f, 1.1f, 1f).setDuration(500).start()
    }

    fun isPauseDialogShowing(): Boolean {
        return mPauseDialog.isShowing
    }

    fun hideAll() {
        MyDialogManager.getInstance().hide(mPauseDialog)
        MyDialogManager.getInstance().hide(mWinDialog)
        MyDialogManager.getInstance().hide(mLoseDialog)
    }

    private fun initPauseDialog() {
        val pauseDialogBinding = DialogPauseBinding.inflate(mActivity.layoutInflater)
        mPauseDialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_pause, pauseDialogBinding.root)
        mPauseDialog.setCanceledOnTouchOutside(false)

        pauseDialogBinding.pauseClose.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mPauseDialog)
            mSetPaused(false)
            mStartCountdownService()
        }

        pauseDialogBinding.pauseRestart.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mPauseDialog)
            mRunAfterClearingHistory {
                startReplacementPlayActivity(mGetLevel())
            }
        }

        pauseDialogBinding.pauseBack.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mPauseDialog)
            mRunAfterClearingHistory {
                mActivity.startActivityWithTransition(
                    MapRoute.createForLevel(mActivity, mGetLevel(), flashHome = true),
                    R.anim.playpage_show,
                    R.anim.playpage_hide
                )
                mActivity.finish()
            }
        }

        initSettingsButtons(pauseDialogBinding)
    }

    private fun initSettingsButtons(pauseDialogBinding: DialogPauseBinding) {
        val musicBtn = pauseDialogBinding.pauseMusic
        musicBtn.setImageResource(if (mMusicOpened) R.drawable.icon_music_on else R.drawable.icon_music_off)
        musicBtn.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val prefs = mActivity.getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
            if (mMusicOpened) {
                musicBtn.setImageResource(R.drawable.icon_music_off)
                mMusicOpened = false
                prefs.edit { putBoolean(AppSettings.KEY_MUSIC, false) }
                PlayMusic.getInstance().stopBGM()
            } else {
                musicBtn.setImageResource(R.drawable.icon_music_on)
                mMusicOpened = true
                prefs.edit { putBoolean(AppSettings.KEY_MUSIC, true) }
                PlayMusic.getInstance().playBGM()
            }
        }

        val audioBtn = pauseDialogBinding.pauseAudio
        audioBtn.setImageResource(if (mAudioOpened) R.drawable.icon_sound_on else R.drawable.icon_sound_off)
        audioBtn.setOnClickListener {
            val prefs = mActivity.getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
            if (mAudioOpened) {
                audioBtn.setImageResource(R.drawable.icon_sound_off)
                mAudioOpened = false
                prefs.edit { putBoolean(AppSettings.KEY_AUDIO, false) }
            } else {
                audioBtn.setImageResource(R.drawable.icon_sound_on)
                mAudioOpened = true
                prefs.edit { putBoolean(AppSettings.KEY_AUDIO, true) }
                PlayMusic.getInstance().playButtonTap()
            }
        }
    }

    private fun initLoseDialog() {
        val loseDialogBinding = DialogLoseBinding.inflate(mActivity.layoutInflater)
        mLoseDialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_lose, loseDialogBinding.root)
        mLoseDialog.setCanceledOnTouchOutside(false)

        loseDialogBinding.loseClose.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            mRunAfterClearingHistory {
                mActivity.startActivityWithTransition(
                    MapRoute.createAfterLose(mActivity, mGetLevel(), flashHome = true),
                    R.anim.playpage_show,
                    R.anim.playpage_hide
                )
                mActivity.finish()
            }
        }

        loseDialogBinding.loseRetry.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            mRunAfterClearingHistory {
                startReplacementPlayActivity(mGetLevel())
            }
        }
    }

    private fun initWinDialog() {
        mWinDialogBinding = DialogWinBinding.inflate(mActivity.layoutInflater)
        mWinDialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_win, mWinDialogBinding.root)
        mWinDialog.setCanceledOnTouchOutside(false)

        mWinDialogBinding.winClose.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            mRunAfterClearingHistory {
                val level = mGetLevel()
                val intent = MapRoute.createAfterWin(
                    mActivity,
                    nextLevel = if (level < mMaxLevel) level + 1 else null,
                    flashHome = true
                )
                mActivity.startActivityWithTransition(intent, R.anim.playpage_show, R.anim.playpage_hide)
                mActivity.finish()
            }
        }

        mWinDialogBinding.winNext.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            mRunAfterClearingHistory {
                val level = mGetLevel()
                if (level == mMaxLevel) {
                    mActivity.startActivityWithTransition(
                        MapRoute.create(mActivity, flashHome = true),
                        R.anim.playpage_show,
                        R.anim.playpage_hide
                    )
                    mActivity.finish()
                } else {
                    startReplacementPlayActivity(level + 1)
                }
            }
        }
    }

    private fun startReplacementPlayActivity(level: Int) {
        /*
         * 重新进入棋盘页时，旧 Activity 的 onDestroy 不再停止倒计时服务，
         * 避免新棋盘刚启动的服务被旧页面生命周期误杀。
         */
        mPrepareForReplacementPlayActivity()
        mActivity.startActivityWithTransition(
            PlayRoute.create(mActivity, level, mGetUsername()),
            R.anim.playpage_show,
            R.anim.playpage_hide
        )
        mActivity.finish()
    }
}
