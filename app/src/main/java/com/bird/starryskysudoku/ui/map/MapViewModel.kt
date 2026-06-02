package com.bird.starryskysudoku.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.repository.MapRepository
import kotlinx.coroutines.launch

class MapViewModel(private val mMapRepository: MapRepository) : ViewModel() {

    private val mMapDataSource = MutableLiveData<List<Array<MapEntity?>>>()
    val mMapData: LiveData<List<Array<MapEntity?>>> = mMapDataSource

    fun loadMapData(username: String = LauncherSessionReader.GUEST_USERNAME) {
        viewModelScope.launch {
            // 地图数据总是以最新用户名重新拉取，保证游客和登录用户状态隔离。
            mMapDataSource.value = mMapRepository.loadMapData(username)
        }
    }

    fun getPassStatus(
        username: String = LauncherSessionReader.GUEST_USERNAME,
        passNum: Int,
        callback: (String?) -> Unit
    ) {
        viewModelScope.launch {
            // 这里继续保留回调接口，减少旧弹窗控制器的改造范围。
            callback(mMapRepository.getPassStatus(username, passNum))
        }
    }

    fun getPassStatus(passNum: Int, callback: (String?) -> Unit) {
        getPassStatus(LauncherSessionReader.GUEST_USERNAME, passNum, callback)
    }

    fun getPassTimes(
        username: String = LauncherSessionReader.GUEST_USERNAME,
        passNum: Int,
        callback: (String) -> Unit
    ) {
        viewModelScope.launch {
            callback(mMapRepository.getPassTimes(username, passNum))
        }
    }

    fun getPassTimes(passNum: Int, callback: (String) -> Unit) {
        getPassTimes(LauncherSessionReader.GUEST_USERNAME, passNum, callback)
    }

    fun updateStatus(
        username: String = LauncherSessionReader.GUEST_USERNAME,
        passNum: Int,
        status: String
    ) {
        viewModelScope.launch {
            mMapRepository.updateStatus(username, passNum, status)
        }
    }

    fun updateStatus(passNum: Int, status: String) {
        updateStatus(LauncherSessionReader.GUEST_USERNAME, passNum, status)
    }

    fun updateCompleteNum(username: String = LauncherSessionReader.GUEST_USERNAME, passNum: Int) {
        viewModelScope.launch {
            // 这里累加的是进入该关的游玩次数，而不是已通关关卡数。
            mMapRepository.incrementPlayTime(username, passNum)
        }
    }

    fun updateCompleteNum(passNum: Int) {
        updateCompleteNum(LauncherSessionReader.GUEST_USERNAME, passNum)
    }
}
