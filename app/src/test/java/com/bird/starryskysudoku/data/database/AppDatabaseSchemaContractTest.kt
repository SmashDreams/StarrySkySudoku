package com.bird.starryskysudoku.data.database

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppDatabaseSchemaContractTest {
    @Test
    fun roomSchemaExportIsEnabledAndVersionSevenSchemaIsCheckedIn() {
        val databaseSource = File("src/main/java/com/bird/starryskysudoku/data/database/AppDatabase.kt").readText()
        val buildSource = File("build.gradle").readText()

        assertTrue(databaseSource.contains("version = 7"))
        assertTrue(databaseSource.contains("exportSchema = true"))
        assertTrue(buildSource.contains("room.schemaLocation"))
    }
}
