package com.engboost.monitoringdevices.monitoring.ui.bluetooth

import com.engboost.monitoringdevices.monitoring.ui.common.PermissionRequestUi

internal data class BluetoothUiState(
    val statusText: String = "Stopped",
    val devices: List<BluetoothDeviceUi> = emptyList(),
    val selectedDevice: BluetoothDeviceUi? = null,
    val sortMode: BluetoothSortMode = BluetoothSortMode.STABLE,
    val isScanning: Boolean = false,
    val permissionRequest: PermissionRequestUi? = null
)

internal enum class BluetoothSortMode(val title: String) {
    /** Keeps the order in which devices were found. */
    STABLE("Stable"),
    SIGNAL("Signal strength"),
    NAME("Name")
}

internal data class BluetoothDeviceUi(
    val name: String,
    val address: String,
    val rssiDbm: Int,
    val type: String,
    val bondState: String,
    val isStale: Boolean
)
