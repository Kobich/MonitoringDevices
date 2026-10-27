package com.engboost.monitoringdevices.features.location

enum class LocationSource {
    /** Android's combination of satellites, Wi-Fi, cell towers and sensors. */
    FUSED,

    GPS,

    /** Wi-Fi and cell towers; needs internet. */
    NETWORK
}

/** [accuracyMeters] is Android's accuracy: about 68% confidence, not a hard bound. */
internal data class LocationSample(
    val source: LocationSource,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val elapsedRealtimeMillis: Long
)
