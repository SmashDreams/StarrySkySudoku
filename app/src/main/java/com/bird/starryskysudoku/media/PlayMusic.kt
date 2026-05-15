package com.bird.starryskysudoku.media

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.bird.starryskysudoku.R

class PlayMusic private constructor() {

    companion object {
        private const val TAG = "PlayMusic"
        private const val PREFS_NAME = "music_set"
        private const val KEY_MUSIC = "music"
        private const val KEY_AUDIO = "audio"

        @Volatile
        private var instance: PlayMusic? = null

        fun getInstance(): PlayMusic {
            return instance ?: synchronized(this) {
                instance ?: PlayMusic().also { instance = it }
            }
        }
    }

    enum class MusicType {
        BUTTONTAP, WRONG, DIALOGSHOW,
        WINNING, LOSING, GETSTAR,
        MAPLIGHTSTAR, BGM, TIMESUP
    }

    private lateinit var soundPool: SoundPool
    private lateinit var timesUpPool: SoundPool
    private var bgmPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences
    private val musicMap = mutableMapOf<MusicType, Int>()
    private val soundIdMap = mutableMapOf<MusicType, Int>()
    private val streamMap = mutableMapOf<MusicType, Int>()
    private var initialized = false

    fun init(application: Application) {
        if (initialized) return
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttrs)
            .build()

        timesUpPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttrs)
            .build()

        initResource()
        loadSounds(application)
        initialized = true
    }

    private fun initResource() {
        musicMap[MusicType.BUTTONTAP] = R.raw.button_tap
        musicMap[MusicType.WRONG] = R.raw.wrong_placement
        musicMap[MusicType.DIALOGSHOW] = R.raw.popup_appear
        musicMap[MusicType.WINNING] = R.raw.puzzle_complete
        musicMap[MusicType.LOSING] = R.raw.puzzle_fail
        musicMap[MusicType.GETSTAR] = R.raw.popup_star
        musicMap[MusicType.MAPLIGHTSTAR] = R.raw.map_star_on
        musicMap[MusicType.BGM] = R.raw.bgm
        musicMap[MusicType.TIMESUP] = R.raw.time
    }

    private fun loadSounds(context: Context) {
        val bgmRes = musicMap[MusicType.BGM]!!
        bgmPlayer = MediaPlayer.create(context, bgmRes).apply {
            isLooping = true
            setVolume(0.2f, 0.2f)
        }

        for ((type, resId) in musicMap) {
            if (type == MusicType.BGM) continue
            val id = if (type == MusicType.TIMESUP) {
                timesUpPool.load(context, resId, 1)
            } else {
                soundPool.load(context, resId, 1)
            }
            soundIdMap[type] = id
        }
        Log.w(TAG, "Load music success!")
    }

    private fun isOpened(type: String): Boolean = prefs.getBoolean(type, true)

    fun playBGM() {
        if (!initialized) return
        try {
            if (isOpened(KEY_MUSIC) && bgmPlayer?.isPlaying == false) {
                bgmPlayer?.start()
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun stopBGM() {
        if (!initialized) return
        try { bgmPlayer?.pause() } catch (e: IllegalStateException) { e.printStackTrace() }
    }

    fun playButtonTap() {
        if (initialized && isOpened(KEY_AUDIO)) {
            soundPool.play(soundIdMap[MusicType.BUTTONTAP] ?: return, 0.4f, 0.4f, 1, 0, 1f)
        }
    }

    fun playDialogShow() { if (isOpened(KEY_AUDIO)) playSound(MusicType.DIALOGSHOW) }

    fun stopDialogShow() {
        if (!initialized) return
        soundPool.play(soundIdMap[MusicType.BUTTONTAP] ?: return, 0f, 0f, 1, 0, 1f)
    }

    fun playWinning() {
        if (isOpened(KEY_AUDIO)) streamMap[MusicType.WINNING] = playSound(MusicType.WINNING)
    }

    fun stopWinning() { if (initialized) streamMap[MusicType.WINNING]?.let { soundPool.stop(it) } }

    fun playLosing() {
        if (isOpened(KEY_AUDIO)) streamMap[MusicType.LOSING] = playSound(MusicType.LOSING)
    }

    fun stopLosing() { if (initialized) streamMap[MusicType.LOSING]?.let { soundPool.stop(it) } }

    fun playGetStar() { if (isOpened(KEY_AUDIO)) playSound(MusicType.GETSTAR) }

    fun playInputWrong() {
        if (initialized && isOpened(KEY_AUDIO)) {
            soundPool.play(soundIdMap[MusicType.WRONG] ?: return, 0.6f, 0.6f, 1, 0, 1f)
        }
    }

    fun playTimesUp() {
        if (initialized && isOpened(KEY_AUDIO)) {
            streamMap[MusicType.TIMESUP] = timesUpPool.play(
                soundIdMap[MusicType.TIMESUP] ?: return, 1f, 1f, 1, 0, 1f)
        }
    }

    fun stopTimesUp() { if (initialized) streamMap[MusicType.TIMESUP]?.let { timesUpPool.stop(it) } }

    fun playMapLightStar() { if (isOpened(KEY_AUDIO)) playSound(MusicType.MAPLIGHTSTAR) }

    fun release() {
        if (!initialized) return
        try {
            soundPool.release()
            timesUpPool.release()
            bgmPlayer?.apply { stop(); release() }
        } catch (e: IllegalStateException) { e.printStackTrace() }
        bgmPlayer = null
        soundIdMap.clear()
        streamMap.clear()
        initialized = false
    }

    private fun playSound(type: MusicType): Int {
        if (!initialized) return 0
        return soundPool.play(soundIdMap[type] ?: return 0, 1f, 1f, 1, 0, 1f)
    }
}
