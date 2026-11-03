package com.engboost.monitoringdevices.core.document.impl.image

internal object BitmapSampling {
    /**
     * Largest power of two (`BitmapFactory.Options.inSampleSize`) for an image of
     * [width] x [height] that will be scaled to fit into [maxWidth] x [maxHeight]:
     * the decoded bitmap stays at least as large as the fitted result, never smaller.
     */
    fun sampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
        val boxWidth = maxWidth.coerceAtLeast(1)
        val boxHeight = maxHeight.coerceAtLeast(1)
        var sample = 1
        // The fitted size is limited by one side only, so it is enough that one side stays above its limit.
        while (width / (sample * 2) >= boxWidth || height / (sample * 2) >= boxHeight) {
            sample *= 2
        }
        return sample
    }
}
