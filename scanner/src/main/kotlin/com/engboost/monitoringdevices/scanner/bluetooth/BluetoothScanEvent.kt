package com.engboost.monitoringdevices.scanner.bluetooth

sealed interface BluetoothScanEvent {
    data object Scanning : BluetoothScanEvent

    /** The full list of devices seen within [BluetoothScanConfig.retentionMillis], not a delta. */
    data class Devices(
        val devices: List<ScannedBluetoothDevice>
    ) : BluetoothScanEvent

    /** Terminal event. */
    data class PermissionRequired(
        val permissions: Set<String>
    ) : BluetoothScanEvent

    /** Terminal event. */
    data class Unavailable(
        val reason: BluetoothUnavailableReason
    ) : BluetoothScanEvent

    /** Terminal event. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : BluetoothScanEvent
}

enum class BluetoothUnavailableReason {
    ADAPTER_UNAVAILABLE,
    BLUETOOTH_DISABLED,
    LE_SCANNER_UNAVAILABLE
}
