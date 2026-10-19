package com.engboost.monitoringdevices.scanner.bluetooth

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** Uses a Bluetooth LE scan, which also finds dual-mode devices; Bluetooth Classic is not used. */
interface BluetoothScannerApi {
    val requiredPermissions: Set<String>

    fun missingPermissions(): Set<String>

    /**
     * Scans while the returned cold flow is collected.
     *
     * The flow completes by itself only after a terminal event.
     */
    fun scan(): Flow<BluetoothScanEvent>
}

fun BluetoothScannerApi(
    context: Context,
    config: BluetoothScanConfig = BluetoothScanConfig()
): BluetoothScannerApi = BluetoothScannerImpl(context, config)
