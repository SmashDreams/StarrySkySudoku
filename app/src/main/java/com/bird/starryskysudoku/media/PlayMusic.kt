package com.bird.starryskysudoku.media

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.bird.starryskysudoku.AppSettings
import com.bird.starryskysudoku.R

class PlayMusic private constructor() {

    companion object {
        private const val TAG = "PlayMusic"

        @Volatile
        private var sInstance: PlayMusic? = null

        fun getInstance(): PlayMusic {
            return sInstance ?: synchronized(this) {
                sInstance ?: PlayMusic().also { sInstance = it }
            }
        }
    }

    enum class MusicType {
        BUTTONTAP, WRONG, DIALOGSHOW,
        WINNING, LOSING, GETSTAR,
        MAPLIGHTSTAR, BGM, TIMESUP
    }

    private lateinit var mSoundPool: SoundPool
    private lateinit var mTimesUpPool: SoundPool
    private var mBgmPlayer: MediaPlayer? = null
    private lateinit var mPrefs: SharedPreferences
    private val mMusicMap = mutableMapOf<MusicType, Int>()
    private val mSoundIdMap = mutableMapOf<MusicType, Int>()
    private val mStreamMap = mutableMapOf<MusicType, Int>()
    private var mInitialized = false

    fun init(application: Application) {
        if (mInitialized) return
        mPrefs = application.getSharedPreferences(AppSettings.PREFS_MUSIC, Context.MODE_PRIVATE)

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        mSoundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttrs)
            .build()

        mTimesUpPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttrs)
            .build()

        initResource()
        loadSounds(application)
        mInitialized = true
    }

    private fun initResource() {
        mMusicMap[MusicType.BUTTONTAP] = R.raw.button_tap
        mMusicMap[MusicType.WRONG] = R.raw.wrong_placement
        mMusicMap[MusicType.DIALOGSHOW] = R.raw.popup_appear
        mMusicMap[MusicType.WINNING] = R.raw.puzzle_complete
        mMusicMap[MusicType.LOSING] = R.raw.puzzle_fail
        mMusicMap[MusicType.GETSTAR] = R.raw.popup_star
        mMusicMap[MusicType.MAPLIGHTSTAR] = R.raw.map_star_on
        mMusicMap[MusicType.BGM] = R.raw.bgm
        mMusicMap[MusicType.TIMESUP] = R.raw.time
    }

    private fun loadSounds(context: Context) {
        val bgmRes = mMusicMap[MusicType.BGM]!!
        mBgmPlayer = MediaPlayer.create(context, bgmRes).apply {
            isLooping = true
            setVolume(0.2f, 0.2f)
        }

        for ((type, resId) in mMusicMap) {
            if (type == MusicType.BGM) continue
            val id = if (type == MusicType.TIMESUP) {
                mTimesUpPool.load(context, resId, 1)
            } else {
                mSoundPool.load(context, resId, 1)
            }
            mSoundIdMap[type] = id
        }
        Log.w(TAG, "Load music success!")
    }

    private fun isOpened(type: String): Boolean = mPrefs.getBoolean(type, true)

    fun playBGM() {
        if (!mInitialized) return
        try {
            if (isOpened(AppSettings.KEY_MUSIC) && mBgmPlayer?.isPlaying == false) {
                mBgmPlayer?.start()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun stopBGM() {
        if (!mInitialized) return
        try { mBgmPlayer?.pause() } catch (e: IllegalStateException) { e.printStackTrace() }
    }

    fun playButtonTap() {
        if (mInitialized && isOpened(AppSettings.KEY_AUDIO)) {
            mSoundPool.play(mSoundIdMap[MusicType.BUTTONTAP] ?: return, 0.4f, 0.4f, 1, 0, 1f)
        }
    }

    fun playDialogShow() { if (isOpened(AppSettings.KEY_AUDIO)) playSound(MusicType.DIALOGSHOW) }

    fun stopDialogShow() {
        if (!mInitialized) return
        mSoundPool.play(mSoundIdMap[MusicType.BUTTONTAP] ?: return, 0f, 0f, 1, 0, 1f)
    }

    fun playWinning() {
        if (isOpened(AppSettings.KEY_AUDIO)) mStreamMap[MusicType.WINNING] = playSound(MusicType.WINNING)
    }

    fun stopWinning() { if (mInitialized) mStreamMap[MusicType.WINNING]?.let { mSoundPool.stop(it) } }

    fun playLosing() {
        if (isOpened(AppSettings.KEY_AUDIO)) mStreamMap[MusicType.LOSING] = playSound(MusicType.LOSING)
    }

    fun stopLosing() { if (mInitialized) mStreamMap[MusicType.LOSING]?.let { mSoundPool.stop(it) } }

    fun playGetStar() { if (isOpened(AppSettings.KEY_AUDIO)) playSound(MusicType.GETSTAR) }

    fun playInputWrong() {
        if (mInitialized && isOpened(AppSettings.KEY_AUDIO)) {
            mSoundPool.play(mSoundIdMap[MusicType.WRONG] ?: return, 0.6f, 0.6f, 1, 0, 1f)
        }
    }

    fun playTimesUp() {
        if (mInitialized && isOpened(AppSettings.KEY_AUDIO)) {
            mStreamMap[MusicType.TIMESUP] = mTimesUpPool.play(
                mSoundIdMap[MusicType.TIMESUP] ?: return, 1f, 1f, 1, 0, 1f)
        }
    }

    fun stopTimesUp() { if (mInitialized) mStreamMap[MusicType.TIMESUP]?.let { mTimesUpPool.stop(it) } }

    fun playMapLightStar() { if (isOpened(AppSettings.KEY_AUDIO)) playSound(MusicType.MAPLIGHTSTAR) }

    fun release() {
        if (!mInitialized) return
        try {
            mSoundPool.release()
            mTimesUpPool.release()
            mBgmPlayer?.apply { stop(); release() }
        } catch (e: IllegalStateException) { e.printStackTrace() }
        mBgmPlayer = null
        mSoundIdMap.clear()
        mStreamMap.clear()
        mInitialized = false
    }

    private fun playSound(type: MusicType): Int {
        if (!mInitialized) return 0
        return mSoundPool.play(mSoundIdMap[type] ?: return 0, 1f, 1f, 1, 0, 1f)
    }
}
