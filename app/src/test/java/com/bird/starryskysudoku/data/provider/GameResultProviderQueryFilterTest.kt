package com.bird.starryskysudoku.data.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameResultProviderQueryFilterTest {

    @Test
    fun resolveResultsQueryAllowsUnfilteredCollectionQuery() {
        assertEquals(ResultQueryFilter.All, GameResultProvider.resolveResultsQueryFilter(null, null))
    }

    @Test
    fun resolveResultsQueryTrimsUsernameForUsernameSelection() {
        assertEquals(
            ResultQueryFilter.Username("alice"),
            GameResultProvider.resolveResultsQueryFilter(
                GameResultContract.Results.selectionForUsername(),
                arrayOf(" alice ")
            )
        )
    }

    @Test
    fun resolveResultsQueryRejectsBlankUsernameSelectionArg() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultProvider.resolveResultsQueryFilter(
                GameResultContract.Results.selectionForUsername(),
                arrayOf("   ")
            )
        }
    }

    @Test
    fun resolveResultsQueryRejectsMissingUsernameSelectionArg() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultProvider.resolveResultsQueryFilter(
                GameResultContract.Results.selectionForUsername(),
                emptyArray()
            )
        }
    }

    @Test
    fun resolveResultsQueryRejectsUnsupportedSelection() {
        assertThrows(IllegalArgumentException::class.java) {
            GameResultProvider.resolveResultsQueryFilter("level=?", arrayOf("1"))
        }
    }
}
