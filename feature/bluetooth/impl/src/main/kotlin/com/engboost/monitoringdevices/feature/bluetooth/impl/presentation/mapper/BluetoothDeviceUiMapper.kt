package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.mapper

import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothDeviceUi
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothSortMode
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothBondState
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceInfo
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceType

internal class BluetoothDeviceUiMapper {
    fun map(
        devices: List<BluetoothDeviceInfo>,
        sortMode: BluetoothSortMode,
        currentDevices: List<BluetoothDeviceUi>
    ): List<BluetoothDeviceUi> {
        val mappedDevices = devices.map { device ->
            BluetoothDeviceUi(
                name = device.name?.takeIf { name -> name.isNotBlank() } ?: "Unknown device",
                address = device.address,
                rssiDbm = device.rssiDbm,
                type = device.type.toDisplayName(),
                bondState = device.bondState.toDisplayName(),
                lastSeenAgoMillis = device.lastSeenAgoMillis
            )
        }

        return sort(mappedDevices, sortMode, currentDevices)
    }

    fun sort(
        devices: List<BluetoothDeviceUi>,
        sortMode: BluetoothSortMode,
        currentDevices: List<BluetoothDeviceUi> = devices
    ): List<BluetoothDeviceUi> {
        return when (sortMode) {
            BluetoothSortMode.STABLE -> {
                val devicesByAddress = devices.associateBy { device -> device.address }
                val currentAddresses = currentDevices
                    .mapTo(hashSetOf()) { device -> device.address }

                currentDevices.mapNotNull { device -> devicesByAddress[device.address] } +
                    devices.filterNot { device -> device.address in currentAddresses }
            }

            BluetoothSortMode.SIGNAL -> devices.sortedByDescending { device ->
                device.rssiDbm ?: Int.MIN_VALUE
            }

            BluetoothSortMode.NAME -> devices.sortedWith(
                compareBy<BluetoothDeviceUi> { device -> device.name == "Unknown device" }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { device -> device.name }
                    .thenBy { device -> device.address }
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
