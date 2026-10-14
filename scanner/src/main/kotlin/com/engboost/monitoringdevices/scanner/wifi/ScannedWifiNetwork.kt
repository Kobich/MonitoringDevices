package com.engboost.monitoringdevices.scanner.wifi

/**
 * Wi-Fi access point found by the last scan.
 *
 * @property ssid network name, or `null` for a hidden network.
 * @property bssid MAC address of the access point.
 * @property rssiDbm signal level in dBm.
 * @property frequencyMhz channel frequency in MHz.
 * @property capabilities authentication, key management and encryption schemes, e.g. `[WPA2-PSK-CCMP]`.
 */
data class ScannedWifiNetwork(
    val ssid: String?,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val capabilities: String
)
