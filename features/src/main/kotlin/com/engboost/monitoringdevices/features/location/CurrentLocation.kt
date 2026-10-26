package com.engboost.monitoringdevices.features.location

/** The best location found: where it came from and how old it is. */
data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val source: LocationSource,
    val ageMillis: Long
)
