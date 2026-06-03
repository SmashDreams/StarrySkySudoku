package com.bird.starryskysudoku.ui.guide

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideDesignCanvasTest {

    @Test
    fun `fills sixteen by nine parent without offset`() {
        val frame = GuideDesignCanvas.fit(parentWidth = 720, parentHeight = 1280)

        assertEquals(1f, frame.mScale, 0.001f)
        assertEquals(720, frame.mWidth)
        assertEquals(1280, frame.mHeight)
        assertEquals(0, frame.mLeft)
        assertEquals(0, frame.mTop)
    }

    @Test
    fun `centers canvas vertically on tall screens`() {
        val frame = GuideDesignCanvas.fit(parentWidth = 1080, parentHeight = 2400)

        assertEquals(1.5f, frame.mScale, 0.001f)
        assertEquals(1080, frame.mWidth)
        assertEquals(1920, frame.mHeight)
        assertEquals(0, frame.mLeft)
        assertEquals(240, frame.mTop)
    }

    @Test
    fun `scales down for smaller sixteen by nine parents`() {
        val frame = GuideDesignCanvas.fit(parentWidth = 360, parentHeight = 640)

        assertEquals(0.5f, frame.mScale, 0.001f)
        assertEquals(360, frame.mWidth)
        assertEquals(640, frame.mHeight)
        assertEquals(0, frame.mLeft)
        assertEquals(0, frame.mTop)
    }

    @Test
    fun `centers canvas horizontally on wide screens`() {
        val frame = GuideDesignCanvas.fit(parentWidth = 1400, parentHeight = 1280)

        assertEquals(1f, frame.mScale, 0.001f)
        assertEquals(720, frame.mWidth)
        assertEquals(1280, frame.mHeight)
        assertEquals(340, frame.mLeft)
        assertEquals(0, frame.mTop)
    }
}
