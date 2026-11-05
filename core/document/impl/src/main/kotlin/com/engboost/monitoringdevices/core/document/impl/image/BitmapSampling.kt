package com.engboost.monitoringdevices.core.document.impl.image

internal object BitmapSampling {
    /** Power of two that keeps the decoded bitmap at least as large as the image fitted into the box. */
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
