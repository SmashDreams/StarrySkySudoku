package com.bird.starryskysudoku.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bird.starryskysudoku.account.LauncherSessionReader
import com.bird.starryskysudoku.data.database.AppDatabase
import com.bird.starryskysudoku.data.entity.MapEntity
import com.bird.starryskysudoku.data.entity.UserMapEntity
import kotlinx.coroutines.launch

class MapViewModel(private val mDb: AppDatabase) : ViewModel() {

    private val mMapDataSource = MutableLiveData<List<Array<MapEntity?>>>()
    val mMapData: LiveData<List<Array<MapEntity?>>> = mMapDataSource

    fun loadMapData(username: String = LauncherSessionReader.GUEST_USERNAME) {
        viewModelScope.launch {
            val safeUsername = ensureUserMap(username)
            val allMaps = mDb.userMapDao().getAllForUser(safeUsername).map { it.toMapEntity() }
            val grouped = allMaps.chunked(4).map { chunk ->
                arrayOf(
                    chunk.getOrNull(0), chunk.getOrNull(1),
                    chunk.getOrNull(2), chunk.getOrNull(3)
                )
            }
            mMapDataSource.value = grouped
        }
    }

    fun getPassStatus(
        username: String = LauncherSessionReader.GUEST_USERNAME,
        passNum: Int,
        callback: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val safeUsername = ensureUserMap(username)
            callback(mDb.userMapDao().getByUserAndPass(safeUsername, passNum)?.mStatus)
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
            val safeUsername = ensureUserMap(username)
            callback(mDb.userMapDao().getByUserAndPass(safeUsername, passNum)?.mPlayTime ?: "0")
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
            val safeUsername = ensureUserMap(username)
            mDb.userMapDao().updateStatus(safeUsername, passNum, status)
        }
    }

    fun updateStatus(passNum: Int, status: String) {
        updateStatus(LauncherSessionReader.GUEST_USERNAME, passNum, status)
    }

    fun updateCompleteNum(username: String = LauncherSessionReader.GUEST_USERNAME, passNum: Int) {
        viewModelScope.launch {
            val safeUsername = ensureUserMap(username)
            val map = mDb.userMapDao().getByUserAndPass(safeUsername, passNum)
            val newTimes = ((map?.mPlayTime?.toIntOrNull() ?: 0) + 1).toString()
            mDb.userMapDao().updatePlayTime(safeUsername, passNum, newTimes)
        }
    }

    fun updateCompleteNum(passNum: Int) {
        updateCompleteNum(LauncherSessionReader.GUEST_USERNAME, passNum)
    }

    private suspend fun ensureUserMap(username: String): String {
        val safeUsername = username.trim().ifEmpty { LauncherSessionReader.GUEST_USERNAME }
        if (mDb.userMapDao().getAllForUser(safeUsername).isNotEmpty()) return safeUsername

        val userRows = mDb.mapDao().getAllMaps().mapIndexed { index, map ->
            UserMapEntity(
                mUsername = safeUsername,
                mPassNum = map.mPassNum,
                mStatus = if (index == 0) "待通关" else "未通关",
                mPlayTime = "0"
            )
        }
        mDb.userMapDao().insertAll(userRows)
        return safeUsername
    }

    private fun UserMapEntity.toMapEntity() = MapEntity(
        mPassNum = mPassNum,
        mStatus = mStatus,
        mPlayTime = mPlayTime
    )
}
