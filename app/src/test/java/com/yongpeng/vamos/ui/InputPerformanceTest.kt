// Modified by Vamos (2026): phantom IME and bounded decoding regressions.
package com.yongpeng.vamos.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class InputPerformanceTest {
    @Test fun hiddenImeNeverReservesPhantomSpace() {
        assertEquals(0, visibleImeHeight(false, 900, 60))
        assertEquals(840, visibleImeHeight(true, 900, 60))
        assertEquals(0, visibleImeHeight(true, 0, 60)) // floating/hardware IME
    }
    @Test fun screenshotDecodeKeepsEnoughPixelsWithoutFullResolution() {
        assertEquals(4, screenshotSampleSize(2160, 440))
        assertEquals(1, screenshotSampleSize(320, 440))
        assertEquals(2, screenshotSampleSize(1080, 540))
    }
}
