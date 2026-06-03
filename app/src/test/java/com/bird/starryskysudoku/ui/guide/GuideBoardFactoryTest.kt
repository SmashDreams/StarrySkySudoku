package com.bird.starryskysudoku.ui.guide

import com.bird.starryskysudoku.ui.play.PlayViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class GuideBoardFactoryTest {

    private val mFirstLevelValues = List(81) { index ->
        when (index) {
            0 -> 8
            1 -> 4
            3 -> 7
            10 -> 2
            else -> 0
        }
    }

    @Test
    fun `welcome board uses supplied level one values instead of hardcoded puzzle`() {
        val board = GuideBoardFactory.createBoard(mFirstLevelValues, GuideStep.WELCOME)

        assertEquals("8", board[0][0].mValue)
        assertEquals("4", board[0][1].mValue)
        assertEquals("7", board[0][3].mValue)
        assertEquals("2", board[1][1].mValue)
        assertEquals("0", board[8][8].mValue)
        assertEquals(PlayViewModel.PROBLEM, board[0][0].mType)
        assertEquals(PlayViewModel.EMPTY, board[0][2].mType)
    }

    @Test
    fun `select cell step highlights the selected empty cell and related row column and block`() {
        val board = GuideBoardFactory.createBoard(mFirstLevelValues, GuideStep.SELECT_CELL)

        assertEquals(PlayViewModel.BE_SELECTED, board[4][4].mStatus)
        assertEquals(PlayViewModel.SELECT_ON, board[4][7].mStatus)
        assertEquals(PlayViewModel.SELECT_ON, board[7][4].mStatus)
        assertEquals(PlayViewModel.SELECT_ON, board[3][3].mStatus)
        assertEquals(PlayViewModel.SELECT_NONE, board[0][8].mStatus)
    }

    @Test
    fun `enter number step fills the demo selected empty cell without changing it into a given clue`() {
        val board = GuideBoardFactory.createBoard(mFirstLevelValues, GuideStep.ENTER_NUMBER)

        assertEquals("7", board[4][4].mValue)
        assertEquals(PlayViewModel.EMPTY, board[4][4].mType)
        assertEquals(PlayViewModel.BE_SELECTED, board[4][4].mStatus)
    }
}
