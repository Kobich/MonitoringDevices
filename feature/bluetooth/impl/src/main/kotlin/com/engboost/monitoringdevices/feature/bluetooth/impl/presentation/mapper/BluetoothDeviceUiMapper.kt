package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.mapper

import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothDeviceUi
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothBondState
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceInfo
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceType

internal class BluetoothDeviceUiMapper {
    fun map(devices: List<BluetoothDeviceInfo>): List<BluetoothDeviceUi> {
        return devices
            .sortedByDescending { device -> device.rssiDbm ?: Int.MIN_VALUE }
            .map { device ->
                BluetoothDeviceUi(
                    name = device.name?.takeIf { name -> name.isNotBlank() } ?: "Unknown device",
                    address = device.address,
                    rssiDbm = device.rssiDbm,
                    type = device.type.toDisplayName(),
                    bondState = device.bondState.toDisplayName()
                )
            }
    }

    private fun BluetoothDeviceType.toDisplayName(): String {
        return when (this) {
            BluetoothDeviceType.CLASSIC -> "Classic"
            BluetoothDeviceType.BLE -> "BLE"
            BluetoothDeviceType.DUAL -> "Dual"
            BluetoothDeviceType.UNKNOWN -> "Unknown"
        }
    }

    private fun BluetoothBondState.toDisplayName(): String {
        return when (this) {
            BluetoothBondState.NONE -> "Not bonded"
            BluetoothBondState.BONDING -> "Bonding"
            BluetoothBondState.BONDED -> "Bonded"
            BluetoothBondState.UNKNOWN -> "Unknown"
        }
    }
}
