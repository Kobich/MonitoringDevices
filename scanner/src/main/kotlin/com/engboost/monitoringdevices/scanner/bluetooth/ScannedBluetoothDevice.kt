package com.engboost.monitoringdevices.scanner.bluetooth

import java.util.UUID

/**
 * @property name `null` for devices that neither advertise a name nor have one cached by Android,
 * e.g. sensors that broadcast only their address.
 * @property rssiDbm smoothed, see [BluetoothScanConfig.rssiSmoothingFactor].
 * @property isStale the device has not been seen for [BluetoothScanConfig.staleAfterMillis].
 * @property manufacturerData company identifier assigned by the Bluetooth SIG mapped to the raw payload
 * as an uppercase hex string. The payload format is vendor-specific, so it is not decoded.
 */
data class ScannedBluetoothDevice(
    val name: String?,
    val address: String,
    val rssiDbm: Int,
    val type: BluetoothDeviceType,
    val bondState: BluetoothBondState,
    val lastSeenAgoMillis: Long,
    val isStale: Boolean,
    val manufacturerData: Map<Int, String> = emptyMap(),
    val serviceUuids: List<UUID> = emptyList()
)

enum class BluetoothDeviceType {
    LE,
    DUAL,
    CLASSIC,
    UNKNOWN
}

enum class BluetoothBondState {
    NONE,
    BONDING,
    BONDED,
    UNKNOWN
}
