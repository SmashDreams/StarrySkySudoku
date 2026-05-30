package com.bird.starryskysudoku.ui.play

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bird.starryskysudoku.data.database.AppDatabase

class PlayViewModelFactory(
    private val mDb: AppDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayViewModel::class.java)) {
            return PlayViewModel(mDb) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel class: ${modelClass.name}")
    }
}
