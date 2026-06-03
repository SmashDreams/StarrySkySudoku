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

    // 地图页列表已经按适配器需要的二维结构整好形，页面只负责观察并渲染。
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
            // 次数查询仍保留回调风格，和旧弹窗绑定代码保持最小改动。
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
            // 地图状态写入统一经由仓储层，避免页面直接感知数据库结构。
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
