package com.engboost.monitoringdevices.monitoring.ble

sealed interface BleScanEvent {
    data object Scanning : BleScanEvent

    data class Devices(
        val devices: List<BleDevice>
    ) : BleScanEvent

    data class PermissionRequired(
        val permissions: Set<String>
    ) : BleScanEvent

    data class Unavailable(
        val reason: BleUnavailableReason
    ) : BleScanEvent

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : BleScanEvent
}

enum class BleUnavailableReason {
    ADAPTER_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    LE_SCANNER_UNAVAILABLE
}

data class BleDevice(
    val name: String?,
    val address: String,
    /** Smoothed RSSI, see [com.engboost.monitoringdevices.monitoring.BleMonitoringConfig]. */
    val rssiDbm: Int,
    val type: BleDeviceType,
    val bondState: BleBondState,
    val lastSeenAgoMillis: Long,
    val isStale: Boolean
)

/** Type reported by the Bluetooth stack. Dual-mode devices are found by BLE scan too. */
enum class BleDeviceType {
    LE,
    DUAL,
    CLASSIC,
    UNKNOWN
}

enum class BleBondState {
    NONE,
    BONDING,
    BONDED,
    UNKNOWN
}
