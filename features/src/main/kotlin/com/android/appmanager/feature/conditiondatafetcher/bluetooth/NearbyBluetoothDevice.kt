package com.android.appmanager.feature.conditiondatafetcher.bluetooth

data class NearbyBluetoothDevice(
    val name: String?,
    val address: String,
    val rssiDbm: Int
)
