package com.bird.starryskysudoku.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.MapEntity
import kotlinx.coroutines.launch

class MapViewModel(private val mDb: AppDatabase) : ViewModel() {

    private val mMapDataSource = MutableLiveData<List<Array<MapEntity?>>>()
    val mMapData: LiveData<List<Array<MapEntity?>>> = mMapDataSource

    fun loadMapData() {
        viewModelScope.launch {
            val allMaps = mDb.mapDao().getAllMaps()
            val grouped = allMaps.chunked(4).map { chunk ->
                arrayOf(
                    chunk.getOrNull(0), chunk.getOrNull(1),
                    chunk.getOrNull(2), chunk.getOrNull(3)
                )
            }
            mMapDataSource.value = grouped
        }
    }

    fun getPassStatus(passNum: Int, callback: (String?) -> Unit) {
        viewModelScope.launch {
            callback(mDb.mapDao().getMapByNum(passNum)?.mStatus)
        }
    }

    fun getPassTimes(passNum: Int, callback: (String) -> Unit) {
        viewModelScope.launch {
            callback(mDb.mapDao().getMapByNum(passNum)?.mPlayTime ?: "0")
        }
    }

    fun updateStatus(passNum: Int, status: String) {
        viewModelScope.launch { mDb.mapDao().updateStatus(passNum, status) }
    }

    fun updateCompleteNum(passNum: Int) {
        viewModelScope.launch {
            val map = mDb.mapDao().getMapByNum(passNum)
            val newTimes = ((map?.mPlayTime?.toIntOrNull() ?: 0) + 1).toString()
            mDb.mapDao().updatePlayTime(passNum, newTimes)
        }
    }
}
