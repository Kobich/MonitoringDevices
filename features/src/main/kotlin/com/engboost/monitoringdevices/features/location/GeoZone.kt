package com.engboost.monitoringdevices.features.location

/** Circle on the map: center in degrees and radius in meters. */
data class GeoZone(
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
) {
    init {
        require(latitude in -90.0..90.0) { "latitude must be in -90..90" }
        require(longitude in -180.0..180.0) { "longitude must be in -180..180" }
        require(radiusMeters > 0) { "radiusMeters must be positive" }
    }
}
