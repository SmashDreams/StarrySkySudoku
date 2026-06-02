package com.bird.starryskysudoku.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.repository.PlayRepository

class PlayViewModelFactory(
    private val mDb: AppDatabase
) : ViewModelProvider.Factory {
    // 与地图页保持同样的工厂注入方式，避免在页面中直接拼装数据仓储细节。
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayViewModel::class.java)) {
            return PlayViewModel(PlayRepository(mDb)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
