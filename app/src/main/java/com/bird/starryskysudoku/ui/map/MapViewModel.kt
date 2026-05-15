package com.bird.starryskysudoku.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.MapEntity
import kotlinx.coroutines.launch

class MapViewModel(private val db: AppDatabase) : ViewModel() {

    private val _mapData = MutableLiveData<List<Array<MapEntity?>>>()
    val mapData: LiveData<List<Array<MapEntity?>>> = _mapData

    fun loadMapData() {
        viewModelScope.launch {
            val allMaps = db.mapDao().getAllMaps()
            val grouped = allMaps.chunked(4).map { chunk ->
                arrayOf(
                    chunk.getOrNull(0), chunk.getOrNull(1),
                    chunk.getOrNull(2), chunk.getOrNull(3)
                )
            }
            _mapData.value = grouped
        }
    }

    fun getPassStatus(passNum: Int, callback: (String?) -> Unit) {
        viewModelScope.launch {
            callback(db.mapDao().getMapByNum(passNum)?.status)
        }
    }

    fun getPassTimes(passNum: Int, callback: (String) -> Unit) {
        viewModelScope.launch {
            callback(db.mapDao().getMapByNum(passNum)?.playTime ?: "0")
        }
    }

    fun updateStatus(passNum: Int, status: String) {
        viewModelScope.launch { db.mapDao().updateStatus(passNum, status) }
    }

    fun updateCompleteNum(passNum: Int) {
        viewModelScope.launch {
            val map = db.mapDao().getMapByNum(passNum)
            val newTimes = ((map?.playTime?.toIntOrNull() ?: 0) + 1).toString()
            db.mapDao().updatePlayTime(passNum, newTimes)
        }
    }
}
