package com.engboost.monitoringdevices.features.location

internal enum class LocationSource {
    /** Satellites: accurate, but can be jammed or spoofed. */
    GPS,

    /** Wi-Fi access points and cell towers: less accurate, not affected by GPS jamming. */
    NETWORK
}

/** One location fix. [accuracyMeters] is the radius the real position is likely within. */
internal data class LocationSample(
    val source: LocationSource,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val elapsedRealtimeMillis: Long,
    val isMock: Boolean
)
