package com.android.appmanager.feature.nearbyscanner.wifi

/**
 * @property ssid `null` for a hidden network.
 * @property bssid MAC address of the access point.
 * @property capabilities raw security schemes as reported by Android, e.g. `[WPA2-PSK-CCMP]`.
 */
data class ScannedWifiNetwork(
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)
