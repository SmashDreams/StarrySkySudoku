package com.bird.starryskysudoku.media

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
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
        BUTTON_TAP, WRONG, DIALOG_SHOW,
        WINNING, LOSING, GET_STAR,
        MAP_LIGHT_STAR, TIMES_UP
    }

    // 资源映射、加载后的音效编号、以及正在播放的流编号分开缓存，便于按类型查找和停止。
    private lateinit var mSoundPool: SoundPool
    private lateinit var mTimesUpPool: SoundPool
    private lateinit var mPrefs: SharedPreferences
    private val mMusicMap = mutableMapOf<MusicType, Int>()
    private val mSoundIdMap = mutableMapOf<MusicType, Int>()
    // 需要显式停止的长一点的提示音，会把流编号缓存在这里。
    private val mStreamMap = mutableMapOf<MusicType, Int>()
    // 仅负责短音效与提示音，背景音乐改由独立服务管理。
    private var mInitialized = false

    fun init(application: Application) {
        if (mInitialized) return
        // 音效开关在应用启动后就要可读，避免页面首次点击时再延迟初始化配置。
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
        // 这里只登记资源映射，真正的加载统一交给后面的音效加载流程处理。
        mMusicMap[MusicType.BUTTON_TAP] = R.raw.button_tap
        mMusicMap[MusicType.WRONG] = R.raw.wrong_placement
        mMusicMap[MusicType.DIALOG_SHOW] = R.raw.popup_appear
        mMusicMap[MusicType.WINNING] = R.raw.puzzle_complete
        mMusicMap[MusicType.LOSING] = R.raw.puzzle_fail
        mMusicMap[MusicType.GET_STAR] = R.raw.popup_star
        mMusicMap[MusicType.MAP_LIGHT_STAR] = R.raw.map_star_on
        mMusicMap[MusicType.TIMES_UP] = R.raw.time
    }

    private fun loadSounds(context: Context) {
        for ((type, resId) in mMusicMap) {
            // 倒计时结束提示音单独使用一个池，避免与普通音效抢占唯一流。
            val id = if (type == MusicType.TIMES_UP) {
                mTimesUpPool.load(context, resId, 1)
            } else {
                mSoundPool.load(context, resId, 1)
            }
            mSoundIdMap[type] = id
        }
        Log.i(TAG, "音效资源加载完成")
    }

    private fun isOpened(type: String): Boolean = mPrefs.getBoolean(type, true)

    fun playButtonTap() {
        if (mInitialized && isOpened(AppSettings.KEY_AUDIO)) {
            mSoundPool.play(mSoundIdMap[MusicType.BUTTON_TAP] ?: return, 0.7f, 0.7f, 1, 0, 1f)
        }
    }

    fun playDialogShow() { if (isOpened(AppSettings.KEY_AUDIO)) playSound(MusicType.DIALOG_SHOW) }

    fun stopDialogShow() {
        if (!mInitialized) return
        // 保持原有静音占位逻辑，立即打断弹窗出现音效。
        mSoundPool.play(mSoundIdMap[MusicType.BUTTON_TAP] ?: return, 0f, 0f, 1, 0, 1f)
    }

    fun playWinning() {
        if (isOpened(AppSettings.KEY_AUDIO)) mStreamMap[MusicType.WINNING] = playSound(MusicType.WINNING)
    }

    fun stopWinning() { if (mInitialized) mStreamMap[MusicType.WINNING]?.let { mSoundPool.stop(it) } }

    fun playLosing() {
        if (isOpened(AppSettings.KEY_AUDIO)) mStreamMap[MusicType.LOSING] = playSound(MusicType.LOSING)
    }

    fun stopLosing() { if (mInitialized) mStreamMap[MusicType.LOSING]?.let { mSoundPool.stop(it) } }

    fun playGetStar() { if (isOpened(AppSettings.KEY_AUDIO)) playSound(MusicType.GET_STAR) }

    fun playInputWrong() {
        if (mInitialized && isOpened(AppSettings.KEY_AUDIO)) {
            mSoundPool.play(mSoundIdMap[MusicType.WRONG] ?: return, 0.9f, 0.9f, 1, 0, 1f)
        }
    }

    fun playTimesUp() {
        if (mInitialized && isOpened(AppSettings.KEY_AUDIO)) {
            // 超时提示音需要支持中途手动停止，所以保留这次播放对应的流编号。
            mStreamMap[MusicType.TIMES_UP] = mTimesUpPool.play(
                mSoundIdMap[MusicType.TIMES_UP] ?: return, 1f, 1f, 1, 0, 1f)
        }
    }

    fun stopTimesUp() {
        if (mInitialized) mStreamMap[MusicType.TIMES_UP]?.let { mTimesUpPool.stop(it) }
    }

    fun playMapLightStar() {
        if (isOpened(AppSettings.KEY_AUDIO)) playSound(MusicType.MAP_LIGHT_STAR)
    }

    fun release() {
        if (!mInitialized) return
        try {
            mSoundPool.release()
            mTimesUpPool.release()
        } catch (e: IllegalStateException) {
            // 释放阶段即使池状态异常，也继续清理本地缓存，避免下次初始化读到脏状态。
            e.printStackTrace()
        }
        // 释放后把缓存索引一起清空，避免旧流编号在下次初始化后被误用。
        mSoundIdMap.clear()
        mStreamMap.clear()
        mInitialized = false
    }

    private fun playSound(type: MusicType): Int {
        if (!mInitialized) return 0
        return mSoundPool.play(mSoundIdMap[type] ?: return 0, 1f, 1f, 1, 0, 1f)
    }
}
