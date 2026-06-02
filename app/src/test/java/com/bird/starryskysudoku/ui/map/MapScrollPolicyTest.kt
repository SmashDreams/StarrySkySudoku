package com.bird.starryskysudoku.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapScrollPolicyTest {
    @Test
    fun completedLevelsThreeToFiveMoveMapByOneHundredTwoDp() {
        assertEquals(102, MapScrollPolicy.offsetDpAfterCompletedLevel(3))
        assertEquals(102, MapScrollPolicy.offsetDpAfterCompletedLevel(4))
        assertEquals(102, MapScrollPolicy.offsetDpAfterCompletedLevel(5))
    }

    @Test
    fun completedLevelsSixToFortyMoveMapByEightyFourDp() {
        assertEquals(84, MapScrollPolicy.offsetDpAfterCompletedLevel(6))
        assertEquals(84, MapScrollPolicy.offsetDpAfterCompletedLevel(39))
        assertEquals(84, MapScrollPolicy.offsetDpAfterCompletedLevel(40))
    }

    @Test
    fun otherLevelsDoNotMoveMap() {
        assertEquals(0, MapScrollPolicy.offsetDpAfterCompletedLevel(1))
        assertEquals(0, MapScrollPolicy.offsetDpAfterCompletedLevel(2))
    }
}
