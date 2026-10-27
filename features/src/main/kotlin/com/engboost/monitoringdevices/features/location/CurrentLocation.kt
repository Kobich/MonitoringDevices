package com.engboost.monitoringdevices.features.location

data class CurrentLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val source: LocationSource,
    val ageMillis: Long
)
