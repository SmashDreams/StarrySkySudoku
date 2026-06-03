package com.bird.starryskysudoku.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.repository.MapRepository

class MapViewModelFactory(
    private val mDb: AppDatabase
) : ViewModelProvider.Factory {
    // 这里手动注入仓储，保持本项目不引入额外依赖注入框架。
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(MapRepository(mDb)) as T
        }
        throw IllegalArgumentException("未知的 ViewModel 类型")
    }
}
