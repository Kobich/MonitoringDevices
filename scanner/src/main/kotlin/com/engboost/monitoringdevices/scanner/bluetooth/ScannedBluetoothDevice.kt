package com.engboost.monitoringdevices.scanner.bluetooth

import java.util.UUID

/**
 * Bluetooth device found by a Bluetooth LE scan.
 *
 * @property name device name, or `null` if it is neither cached by Android nor advertised.
 * @property address MAC address of the device.
 * @property rssiDbm smoothed signal level in dBm, see [BluetoothScanConfig.rssiSmoothingFactor].
 * @property type device type reported by the Bluetooth stack.
 * @property bondState pairing state with this phone.
 * @property lastSeenAgoMillis time since the last advertisement from the device.
 * @property isStale whether the device has not been seen for [BluetoothScanConfig.staleAfterMillis].
 * @property manufacturerData manufacturer-specific data from the last advertisement: a company identifier
 * assigned by the Bluetooth SIG mapped to the payload as an uppercase hex string. The payload format is
 * defined by each manufacturer, so it is not decoded.
 * @property serviceUuids service UUIDs listed in the last advertisement.
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

/** Device type reported by the Bluetooth stack. Dual-mode devices are found by an LE scan too. */
enum class BluetoothDeviceType {
    LE,
    DUAL,
    CLASSIC,
    UNKNOWN
}

/** Pairing state of a device with this phone. */
enum class BluetoothBondState {
    NONE,
    BONDING,
    BONDED,
    UNKNOWN
}
