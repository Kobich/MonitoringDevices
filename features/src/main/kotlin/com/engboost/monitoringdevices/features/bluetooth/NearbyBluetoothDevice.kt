package com.engboost.monitoringdevices.features.bluetooth

data class NearbyBluetoothDevice(
    val name: String?,
    val address: String,
    val rssiDbm: Int
)
