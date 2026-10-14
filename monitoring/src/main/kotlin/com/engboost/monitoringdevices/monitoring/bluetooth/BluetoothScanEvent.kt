package com.engboost.monitoringdevices.monitoring.bluetooth

/** Event of a Bluetooth monitoring session. */
sealed interface BluetoothScanEvent {
    /** The scanner has started; results follow as [Devices]. */
    data object Scanning : BluetoothScanEvent

    /** All devices seen within [BluetoothMonitoringConfig.retentionMillis]. */
    data class Devices(
        val devices: List<BluetoothDeviceInfo>
    ) : BluetoothScanEvent

    /** Terminal event: [permissions] must be granted to continue. */
    data class PermissionRequired(
        val permissions: Set<String>
    ) : BluetoothScanEvent

    /** Terminal event: scanning is impossible in the current device state. */
    data class Unavailable(
        val reason: BluetoothUnavailableReason
    ) : BluetoothScanEvent

    /** Terminal event: scanning failed with [message] and an optional [cause]. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : BluetoothScanEvent
}

/** Reason why Bluetooth scanning is unavailable. */
enum class BluetoothUnavailableReason {
    ADAPTER_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    LE_SCANNER_UNAVAILABLE
}
