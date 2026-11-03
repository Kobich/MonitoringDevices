package com.engboost.monitoringdevices.core.document.impl.image

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapSamplingTest {
    @Test
    fun `48 MP photo fitted into a screen-sized box is decoded at half size`() {
        // 8000 x 6000 into 2160 x 4400: the fitted result is 2160 x 1620, 4000 x 3000 is enough.
        assertEquals(2, BitmapSampling.sampleSize(8000, 6000, 2160, 4400))
    }

    @Test
    fun `panorama is limited by its long side`() {
        // 20000 x 1000 into 1000 x 1000: the fitted result is 1000 x 50.
        assertEquals(16, BitmapSampling.sampleSize(20000, 1000, 1000, 1000))
    }

    @Test
    fun `decoded image is never smaller than the fitted result`() {
        val sample = BitmapSampling.sampleSize(4000, 3000, 1000, 1000)

        assertEquals(4, sample)
        assert(4000 / sample >= 1000)
    }

    @Test
    fun `small image is decoded as is`() {
        assertEquals(1, BitmapSampling.sampleSize(800, 600, 1080, 1920))
    }

    @Test
    fun `zero or negative box doesn't hang or crash`() {
        assertEquals(256, BitmapSampling.sampleSize(500, 500, 0, 0))
        assertEquals(256, BitmapSampling.sampleSize(500, 500, -1, -1))
    }
}
