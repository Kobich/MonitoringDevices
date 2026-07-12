package com.engboost.monitoringdevices.scanner.bluetooth.api

import kotlinx.coroutines.flow.Flow

interface BluetoothScanner {
    val requiredPermissions: Set<String>

    fun scan(config: BluetoothScanConfig = BluetoothScanConfig()): Flow<BluetoothScanEvent>
}

data class BluetoothScanConfig(
    val mode: BluetoothScanMode = BluetoothScanMode.ALL
)

enum class BluetoothScanMode {
    BLE,
    CLASSIC,
    ALL
}

sealed interface BluetoothScanEvent {
    data object Scanning : BluetoothScanEvent

    data class Devices(
        val devices: List<BluetoothDeviceInfo>
    ) : BluetoothScanEvent

    data class PermissionRequired(
        val permissions: Set<String>
    ) : BluetoothScanEvent

    data class Unavailable(
        val reason: String
    ) : BluetoothScanEvent

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : BluetoothScanEvent
}

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val rssiDbm: Int?,
    val type: BluetoothDeviceType,
    val bondState: BluetoothBondState
)

enum class BluetoothDeviceType {
    CLASSIC,
    BLE,
    DUAL,
    UNKNOWN
}

enum class BluetoothBondState {
    NONE,
    BONDING,
    BONDED,
    UNKNOWN
}
