package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model

data class BluetoothUiState(
    val statusText: String,
    val devices: List<BluetoothDeviceUi>,
    val selectedDevice: BluetoothDeviceUi?,
    val sortMode: BluetoothSortMode,
    val isScanning: Boolean,
    val permissionRequest: PermissionRequestUi?
)

enum class BluetoothSortMode {
    STABLE,
    SIGNAL,
    NAME
}

data class BluetoothDeviceUi(
    val name: String,
    val address: String,
    val rssiDbm: Int?,
    val type: String,
    val bondState: String
)

data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
) {
    fun asArray(): Array<String> {
        return permissions.toTypedArray()
    }
}
