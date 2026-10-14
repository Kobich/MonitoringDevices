package com.engboost.monitoringdevices.scanner.ui.bluetooth

import com.engboost.monitoringdevices.scanner.bluetooth.BluetoothUnavailableReason
import com.engboost.monitoringdevices.scanner.ui.common.PermissionRequestUi
import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus

internal data class BluetoothUiState(
    val status: ScanStatus<BluetoothUnavailableReason> = ScanStatus.Stopped,
    val devices: List<BluetoothDeviceUi> = emptyList(),
    val selectedDevice: BluetoothDeviceUi? = null,
    val sortMode: BluetoothSortMode = BluetoothSortMode.STABLE,
    val permissionRequest: PermissionRequestUi? = null
) {
    val isScanning: Boolean
        get() = status == ScanStatus.Scanning
}

/** Order of the device list; [STABLE] keeps the order in which devices were found. */
internal enum class BluetoothSortMode(val title: String) {
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
