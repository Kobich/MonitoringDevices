package com.android.core.document.impl.image

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapSamplingTest {
    @Test
    fun `a large photo is sampled down while staying at least as large as the box`() {
        // 4000 x 3000 / 4 = 1000 x 750: fills the 1000 x 1000 box by width.
        assertEquals(4, BitmapSampling.sampleSize(width = 4000, height = 3000, maxWidth = 1000, maxHeight = 1000))
    }

    @Test
    fun `an image smaller than the box is decoded at full size`() {
        assertEquals(1, BitmapSampling.sampleSize(width = 800, height = 600, maxWidth = 1080, maxHeight = 1920))
    }

    @Test
    fun `a tall image is limited by its height`() {
        // 1000 x 8000 / 4 = 250 x 2000: still fills the 1080 x 1920 box by height.
        assertEquals(4, BitmapSampling.sampleSize(width = 1000, height = 8000, maxWidth = 1080, maxHeight = 1920))
    }
}
