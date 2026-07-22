package com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.mapper

import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothDeviceUi
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.model.BluetoothSortMode
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothBondState
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceInfo
import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothDeviceType

private const val DEVICE_STALE_AFTER_MILLIS = 30_000L
private const val SIGNAL_SORT_BUCKET_DBM = 5

internal class BluetoothDeviceUiMapper {
    fun map(
        devices: List<BluetoothDeviceInfo>,
        sortMode: BluetoothSortMode,
        currentDevices: List<BluetoothDeviceUi>
    ): List<BluetoothDeviceUi> {
        val currentDevicesByAddress = currentDevices.associateBy { device -> device.address }
        val mappedDevices = devices.map { device ->
            val mappedDevice = BluetoothDeviceUi(
                name = device.name?.takeIf { name -> name.isNotBlank() } ?: "Unknown device",
                address = device.address,
                rssiDbm = device.rssiDbm,
                type = device.type.toDisplayName(),
                bondState = device.bondState.toDisplayName(),
                isStale = device.lastSeenAgoMillis?.let { ageMillis ->
                    ageMillis >= DEVICE_STALE_AFTER_MILLIS
                } == true
            )
            currentDevicesByAddress[device.address]
                ?.takeIf { currentDevice -> currentDevice == mappedDevice }
                ?: mappedDevice
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

            BluetoothSortMode.SIGNAL -> {
                val currentOrder = currentDevices
                    .withIndex()
                    .associate { (index, device) -> device.address to index }

                devices.sortedWith(
                    compareByDescending<BluetoothDeviceUi> { device ->
                        device.rssiDbm?.div(SIGNAL_SORT_BUCKET_DBM) ?: Int.MIN_VALUE
                    }.thenBy { device ->
                        currentOrder[device.address] ?: Int.MAX_VALUE
                    }.thenBy { device ->
                        device.address
                    }
                )
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
