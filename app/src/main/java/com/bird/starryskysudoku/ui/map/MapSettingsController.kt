package com.bird.starryskysudoku.ui.map

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R
import com.bird.starryskysudoku.databinding.DialogSettingsBinding
import com.bird.starryskysudoku.media.BgmMusicController
import com.bird.starryskysudoku.media.PlayMusic
import com.bird.starryskysudoku.ui.common.startActivityWithTransition
import com.bird.starryskysudoku.ui.dialog.MyDialog
import com.bird.starryskysudoku.ui.dialog.MyDialogManager
import com.bird.starryskysudoku.ui.howtoplay.HowToPlayActivity

class MapSettingsController(
    private val mActivity: AppCompatActivity,
    private val mSettingsButton: ImageView
) {
    private lateinit var mSettingsDialog: MyDialog
    private var mMusicOpened = true
    private var mAudioOpened = true
    private var mLanguage = AppSettings.DEFAULT_LANGUAGE

    fun init() {
        /*
         * 设置弹窗读写的开关状态与音频模块共用同一组偏好配置。
         */
        val musicPrefs = mActivity.getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
        mMusicOpened = musicPrefs.getBoolean(AppSettings.KEY_MUSIC, true)
        mAudioOpened = musicPrefs.getBoolean(AppSettings.KEY_AUDIO, true)
        mLanguage = mActivity.getSharedPreferences(AppSettings.PREFS_LANGUAGE, Context.MODE_PRIVATE)
            .getString(AppSettings.KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE) ?: AppSettings.DEFAULT_LANGUAGE

        val settingsBinding = DialogSettingsBinding.inflate(mActivity.layoutInflater)
        mSettingsDialog = MyDialogManager.getInstance()
            .initView(mActivity, R.layout.dialog_settings, settingsBinding.root)
        mSettingsDialog.setCanceledOnTouchOutside(true)

        bindDialogActions(settingsBinding)
    }

    fun hide() {
        if (::mSettingsDialog.isInitialized) {
            MyDialogManager.getInstance().hide(mSettingsDialog)
        }
    }

    private fun bindDialogActions(settingsBinding: DialogSettingsBinding) {
        val musicSwitch = settingsBinding.settingsMusic
        val audioSwitch = settingsBinding.settingsAudio

        mSettingsButton.setOnClickListener {
            PlayMusic.getInstance().playDialogShow()
            musicSwitch.setImageResource(
                if (mMusicOpened) R.drawable.icon_music_on else R.drawable.icon_music_off
            )
            audioSwitch.setImageResource(
                if (mAudioOpened) R.drawable.icon_sound_on else R.drawable.icon_sound_off
            )
            MyDialogManager.getInstance().show(mSettingsDialog)
        }

        settingsBinding.settingsClose.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            MyDialogManager.getInstance().hide(mSettingsDialog)
        }

        settingsBinding.settingsHowtoplay.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            mActivity.startActivityWithTransition(
                Intent(mActivity, HowToPlayActivity::class.java),
                R.anim.setguide_right_in,
                R.anim.mappage_gone
            )
        }

        settingsBinding.settingsLanguage.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val newLang = if (mLanguage == "zh") "en" else "zh"
            mLanguage = newLang
            mActivity.getSharedPreferences(AppSettings.PREFS_LANGUAGE, Context.MODE_PRIVATE).edit {
                putString(AppSettings.KEY_LANGUAGE, newLang)
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(newLang))
            MyDialogManager.getInstance().hide(mSettingsDialog)
        }

        musicSwitch.setOnClickListener {
            PlayMusic.getInstance().playButtonTap()
            val prefs = mActivity.getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
            if (mMusicOpened) {
                musicSwitch.setImageResource(R.drawable.icon_music_off)
                mMusicOpened = false
                prefs.edit { putBoolean(AppSettings.KEY_MUSIC, false) }
                BgmMusicController.stop(mActivity)
            } else {
                musicSwitch.setImageResource(R.drawable.icon_music_on)
                mMusicOpened = true
                prefs.edit { putBoolean(AppSettings.KEY_MUSIC, true) }
                BgmMusicController.playIfEnabled(mActivity)
            }
        }

        audioSwitch.setOnClickListener {
            val prefs = mActivity.getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)
            if (mAudioOpened) {
                audioSwitch.setImageResource(R.drawable.icon_sound_off)
                mAudioOpened = false
                prefs.edit { putBoolean(AppSettings.KEY_AUDIO, false) }
            } else {
                audioSwitch.setImageResource(R.drawable.icon_sound_on)
                mAudioOpened = true
                prefs.edit { putBoolean(AppSettings.KEY_AUDIO, true) }
                PlayMusic.getInstance().playButtonTap()
            }
        }
    }

}
