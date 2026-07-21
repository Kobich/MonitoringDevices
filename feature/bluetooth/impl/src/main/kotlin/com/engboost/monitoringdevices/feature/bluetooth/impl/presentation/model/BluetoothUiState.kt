package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model

private const val DEVICE_STALE_AFTER_MILLIS = 30_000L

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
    val bondState: String,
    val lastSeenAgoMillis: Long?
) {
    val isStale: Boolean
        get() = lastSeenAgoMillis?.let { ageMillis ->
            ageMillis >= DEVICE_STALE_AFTER_MILLIS
        } == true
}

data class PermissionRequestUi(
    val id: Int,
    val permissions: List<String>
) {
    fun asArray(): Array<String> {
        return permissions.toTypedArray()
    }
}
