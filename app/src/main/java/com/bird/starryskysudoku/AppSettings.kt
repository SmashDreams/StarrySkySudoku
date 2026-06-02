package com.bird.starryskysudoku

object AppSettings {
    // 语言设置单独存储，应用启动时优先读取这里决定本地化配置。
    const val PREFS_LANGUAGE = "language"
    const val KEY_LANGUAGE = "language"
    const val DEFAULT_LANGUAGE = "zh"

    // 背景音乐与音效开关共用同一组偏好配置。
    const val PREFS_MUSIC = "music_set"
    const val KEY_MUSIC = "music"
    const val KEY_AUDIO = "audio"
}
