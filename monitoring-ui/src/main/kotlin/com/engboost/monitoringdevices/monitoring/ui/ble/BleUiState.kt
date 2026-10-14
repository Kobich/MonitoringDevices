package com.engboost.monitoringdevices.monitoring.ui.ble

internal data class BleUiState(
    val statusText: String,
    val devices: List<BleDeviceUi>,
    val selectedDevice: BleDeviceUi?,
    val sortMode: BleSortMode,
    val isScanning: Boolean,
    val permissionRequest: PermissionRequestUi?
)

internal enum class BleSortMode {
    STABLE,
    SIGNAL,
    NAME
}

internal data class BleDeviceUi(
    val name: String,
    val address: String,
    val rssiDbm: Int,
    val type: String,
    val bondState: String,
    val isStale: Boolean
)

internal data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
) {
    fun asArray(): Array<String> {
        return permissions.toTypedArray()
    }
}
