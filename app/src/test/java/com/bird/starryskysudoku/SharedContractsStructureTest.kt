package com.bird.starryskysudoku

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SharedContractsStructureTest {
    @Test
    fun appIncludesSharedContractSources() {
        val buildFile = File("build.gradle").readText()

        assertTrue(buildFile.contains("../shared-contracts/src/main/java"))
    }

    @Test
    fun localProviderContractsDelegateToSharedContracts() {
        val resultContract = File("src/main/java/com/bird/starryskysudoku/data/provider/GameResultContract.kt").readText()
        val sessionContract = File("src/main/java/com/bird/starryskysudoku/account/LauncherSessionContract.kt").readText()

        assertTrue(resultContract.contains("SharedGameResultsContract"))
        assertTrue(sessionContract.contains("SharedSessionContract"))
    }
}
