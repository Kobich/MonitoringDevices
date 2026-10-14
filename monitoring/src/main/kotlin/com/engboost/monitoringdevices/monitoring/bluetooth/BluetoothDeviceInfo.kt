package com.engboost.monitoringdevices.monitoring.bluetooth

/**
 * Nearby Bluetooth device found by a Bluetooth LE scan.
 *
 * @property name device name, or `null` if it is neither cached by Android nor advertised.
 * @property address MAC address of the device.
 * @property rssiDbm smoothed signal level in dBm, see [BluetoothMonitoringConfig.rssiSmoothingFactor].
 * @property type device type reported by the Bluetooth stack.
 * @property bondState pairing state with this phone.
 * @property lastSeenAgoMillis time since the last advertisement from the device.
 * @property isStale whether the device has not been seen for [BluetoothMonitoringConfig.staleAfterMillis].
 */
data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val rssiDbm: Int,
    val type: BluetoothDeviceType,
    val bondState: BluetoothBondState,
    val lastSeenAgoMillis: Long,
    val isStale: Boolean
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
