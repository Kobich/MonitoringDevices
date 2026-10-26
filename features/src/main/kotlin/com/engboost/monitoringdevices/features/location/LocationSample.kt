package com.engboost.monitoringdevices.features.location

enum class LocationSource {
    /** Android's combined location: satellites, Wi-Fi, cell towers and sensors. */
    FUSED,

    /** Satellites only. */
    GPS,

    /** Wi-Fi access points and cell towers. */
    NETWORK
}

/** One location fix. [accuracyMeters] is the radius the real position is within with about 68% confidence. */
internal data class LocationSample(
    val source: LocationSource,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val elapsedRealtimeMillis: Long
)
