package com.android.appmanager.feature.conditiondatafetcher.wifi

data class WifiNetwork(
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)
