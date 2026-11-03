package com.engboost.monitoringdevices.core.document.impl.image

internal object BitmapSampling {
    /**
     * Largest power of two (`BitmapFactory.Options.inSampleSize`) that keeps an image of
     * [width] x [height] at least [minWidth] x [minHeight] after decoding.
     */
    fun sampleSize(width: Int, height: Int, minWidth: Int, minHeight: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= minWidth && height / (sample * 2) >= minHeight) {
            sample *= 2
        }
        return sample
    }
}
