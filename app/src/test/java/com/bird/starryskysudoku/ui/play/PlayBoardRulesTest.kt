package com.bird.starryskysudoku.ui.play

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayBoardRulesTest {
    @Test
    fun createBoardRejectsInvalidProblemData() {
        assertNull(PlayBoardRules.createBoard(List(80) { 0 }))
        assertNull(PlayBoardRules.createBoard(List(81) { 10 }))
    }

    @Test
    fun createBoardMapsValuesToCellsAndBlocks() {
        val values = List(81) { index -> if (index == 0) 5 else 0 }
        val board = requireNotNull(PlayBoardRules.createBoard(values))

        assertEquals("5", board[0][0].mValue)
        assertEquals(PlayViewModel.PROBLEM, board[0][0].mType)
        assertEquals(1, board[0][0].mBlock)
        assertEquals(9, board[8][8].mBlock)
        assertEquals(PlayViewModel.EMPTY, board[0][1].mType)
    }

    @Test
    fun selectCellHighlightsMatchingNumberOrRelatedEmptyCellPeers() {
        val board = requireNotNull(PlayBoardRules.createBoard(List(81) { index ->
            when (index) {
                0, 10 -> 5
                else -> 0
            }
        }))

        val selectedNumber = PlayBoardRules.selectCell(board, 0, 0)

        assertEquals("5", selectedNumber)
        assertEquals(PlayViewModel.BE_SELECTED, board[0][0].mStatus)
        assertEquals(PlayViewModel.SELECT_ON, board[1][1].mStatus)
        assertEquals(PlayViewModel.SELECT_NONE, board[8][8].mStatus)

        PlayBoardRules.selectCell(board, 0, 1)
        assertEquals(PlayViewModel.BE_SELECTED, board[0][1].mStatus)
        assertEquals(PlayViewModel.SELECT_ON, board[0][8].mStatus)
        assertEquals(PlayViewModel.SELECT_ON, board[8][1].mStatus)
        assertEquals(PlayViewModel.SELECT_ON, board[2][2].mStatus)
    }

    @Test
    fun insertNumberDetectsWrongAndCompletedBoard() {
        val wrongBoard = requireNotNull(PlayBoardRules.createBoard(List(81) { index ->
            if (index == 1) 5 else 0
        }))

        val wrongResult = PlayBoardRules.insertNumber(wrongBoard, 0, 0, "5")

        assertTrue(wrongResult.mIsWrong)
        assertFalse(wrongResult.mIsComplete)
        assertEquals(PlayViewModel.WRONG, wrongBoard[0][0].mStatus)
        assertEquals(PlayViewModel.WRONG, wrongBoard[0][1].mStatus)

        val solvedValues = listOf(
            5, 3, 4, 6, 7, 8, 9, 1, 2,
            6, 7, 2, 1, 9, 5, 3, 4, 8,
            1, 9, 8, 3, 4, 2, 5, 6, 7,
            8, 5, 9, 7, 6, 1, 4, 2, 3,
            4, 2, 6, 8, 5, 3, 7, 9, 1,
            7, 1, 3, 9, 2, 4, 8, 5, 6,
            9, 6, 1, 5, 3, 7, 2, 8, 4,
            2, 8, 7, 4, 1, 9, 6, 3, 5,
            3, 4, 5, 2, 8, 6, 1, 7, 0
        )
        val completeBoard = requireNotNull(PlayBoardRules.createBoard(solvedValues))

        val completeResult = PlayBoardRules.insertNumber(completeBoard, 8, 8, "9")

        assertFalse(completeResult.mIsWrong)
        assertTrue(completeResult.mIsComplete)
    }
}
