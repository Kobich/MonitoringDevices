package com.engboost.monitoringdevices.monitoring.ui.bluetooth

import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothBondState
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothDeviceInfo
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothDeviceType

private const val SIGNAL_SORT_BUCKET_DBM = 5
private const val UNKNOWN_DEVICE_NAME = "Unknown device"

/**
 * Maps scanner devices to UI models and keeps the list order stable between snapshots.
 *
 * Unchanged devices keep the same instances, so Compose skips their recomposition.
 */
internal class BluetoothDeviceUiMapper {
    fun map(
        devices: List<BluetoothDeviceInfo>,
        sortMode: BluetoothSortMode,
        currentDevices: List<BluetoothDeviceUi>
    ): List<BluetoothDeviceUi> {
        val currentDevicesByAddress = currentDevices.associateBy { device -> device.address }
        val mappedDevices = devices.map { device ->
            val mappedDevice = BluetoothDeviceUi(
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

            // Devices within one SIGNAL_SORT_BUCKET_DBM bucket keep their order, so the list does not jump.
            BluetoothSortMode.SIGNAL -> {
                val currentOrder = currentDevices
                    .withIndex()
                    .associate { (index, device) -> device.address to index }

                devices.sortedWith(
                    compareByDescending<BluetoothDeviceUi> { device ->
                        Math.floorDiv(device.rssiDbm, SIGNAL_SORT_BUCKET_DBM)
                    }.thenBy { device ->
                        currentOrder[device.address] ?: Int.MAX_VALUE
                    }.thenBy { device ->
                        device.address
                    }
                )
            }

            BluetoothSortMode.NAME -> devices.sortedWith(
                compareBy<BluetoothDeviceUi> { device -> device.name == UNKNOWN_DEVICE_NAME }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { device -> device.name }
                    .thenBy { device -> device.address }
            )
        }
    }

    private fun BluetoothDeviceType.toDisplayName(): String {
        return when (this) {
            BluetoothDeviceType.LE -> "BLE"
            BluetoothDeviceType.DUAL -> "Dual"
            BluetoothDeviceType.CLASSIC -> "Classic"
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
