package com.engboost.monitoringdevices.monitoring.ui.ble

import com.engboost.monitoringdevices.monitoring.ble.BleBondState
import com.engboost.monitoringdevices.monitoring.ble.BleDevice
import com.engboost.monitoringdevices.monitoring.ble.BleDeviceType

private const val SIGNAL_SORT_BUCKET_DBM = 5
private const val UNKNOWN_DEVICE_NAME = "Unknown device"

internal class BleDeviceUiMapper {
    fun map(
        devices: List<BleDevice>,
        sortMode: BleSortMode,
        currentDevices: List<BleDeviceUi>
    ): List<BleDeviceUi> {
        val currentDevicesByAddress = currentDevices.associateBy { device -> device.address }
        val mappedDevices = devices.map { device ->
            val mappedDevice = BleDeviceUi(
                name = device.name?.takeIf { name -> name.isNotBlank() } ?: UNKNOWN_DEVICE_NAME,
                address = device.address,
                rssiDbm = device.rssiDbm,
                type = device.type.toDisplayName(),
                bondState = device.bondState.toDisplayName(),
                isStale = device.isStale
            )
            currentDevicesByAddress[device.address]
                ?.takeIf { currentDevice -> currentDevice == mappedDevice }
                ?: mappedDevice
        }

        return sort(mappedDevices, sortMode, currentDevices)
    }

    fun sort(
        devices: List<BleDeviceUi>,
        sortMode: BleSortMode,
        currentDevices: List<BleDeviceUi> = devices
    ): List<BleDeviceUi> {
        return when (sortMode) {
            BleSortMode.STABLE -> {
                val devicesByAddress = devices.associateBy { device -> device.address }
                val currentAddresses = currentDevices
                    .mapTo(hashSetOf()) { device -> device.address }

                currentDevices.mapNotNull { device -> devicesByAddress[device.address] } +
                    devices.filterNot { device -> device.address in currentAddresses }
            }

            BleSortMode.SIGNAL -> {
                val currentOrder = currentDevices
                    .withIndex()
                    .associate { (index, device) -> device.address to index }

                devices.sortedWith(
                    compareByDescending<BleDeviceUi> { device ->
                        Math.floorDiv(device.rssiDbm, SIGNAL_SORT_BUCKET_DBM)
                    }.thenBy { device ->
                        currentOrder[device.address] ?: Int.MAX_VALUE
                    }.thenBy { device ->
                        device.address
                    }
                )
            }

            BleSortMode.NAME -> devices.sortedWith(
                compareBy<BleDeviceUi> { device -> device.name == UNKNOWN_DEVICE_NAME }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { device -> device.name }
                    .thenBy { device -> device.address }
            )
        }
    }

    private fun BleDeviceType.toDisplayName(): String {
        return when (this) {
            BleDeviceType.LE -> "BLE"
            BleDeviceType.DUAL -> "Dual"
            BleDeviceType.CLASSIC -> "Classic"
            BleDeviceType.UNKNOWN -> "Unknown"
        }
    }

    private fun BleBondState.toDisplayName(): String {
        return when (this) {
            BleBondState.NONE -> "Not bonded"
            BleBondState.BONDING -> "Bonding"
            BleBondState.BONDED -> "Bonded"
            BleBondState.UNKNOWN -> "Unknown"
        }
    }
}
