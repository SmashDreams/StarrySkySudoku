package com.bird.starryskysudoku.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ViewModelRepositoryBoundaryStructureTest {
    private val mSourceRoot = locateSourceRoot()

    @Test
    fun playAndMapViewModelsDependOnRepositoriesInsteadOfDatabaseOrDao() {
        val playViewModel = mSourceRoot.resolve("ui/play/PlayViewModel.kt").readText()
        val mapViewModel = mSourceRoot.resolve("ui/map/MapViewModel.kt").readText()

        assertFalse(playViewModel.contains("AppDatabase"))
        assertFalse(playViewModel.contains("mDb."))
        assertFalse(mapViewModel.contains("AppDatabase"))
        assertFalse(mapViewModel.contains("mDb."))
        assertTrue(playViewModel.contains("PlayRepository"))
        assertTrue(mapViewModel.contains("MapRepository"))
    }

    @Test
    fun repositoriesOwnRoomDatabaseAccessForPlayAndMapFeatures() {
        val playRepository = mSourceRoot.resolve("data/repository/PlayRepository.kt")
        val mapRepository = mSourceRoot.resolve("data/repository/MapRepository.kt")

        assertTrue(playRepository.isFile)
        assertTrue(mapRepository.isFile)
        assertTrue(playRepository.readText().contains("AppDatabase"))
        assertTrue(mapRepository.readText().contains("AppDatabase"))
    }

    private fun locateSourceRoot(): File {
        var dir = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (true) {
            val sourceRoot = dir.resolve("src/main/java/com/bird/starryskysudoku")
            if (sourceRoot.isDirectory) return sourceRoot
            val appSourceRoot = dir.resolve("app/src/main/java/com/bird/starryskysudoku")
            if (appSourceRoot.isDirectory) return appSourceRoot
            dir = dir.parentFile ?: break
        }
        error("Unable to locate app source root")
    }
}
