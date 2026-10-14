package com.engboost.monitoringdevices.scanner.bluetooth

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Scanner of nearby Bluetooth devices.
 *
 * Uses a Bluetooth LE scan, which finds both LE and dual-mode devices.
 */
interface BluetoothScanner {
    /** Runtime permissions required to scan. */
    val requiredPermissions: Set<String>

    /** Returns permissions from [requiredPermissions] that are not granted yet. */
    fun missingPermissions(): Set<String>

    /**
     * Returns a cold flow that scans while it is collected.
     *
     * The flow completes after a terminal event: [BluetoothScanEvent.PermissionRequired],
     * [BluetoothScanEvent.Unavailable] or [BluetoothScanEvent.Error].
     */
    fun scan(): Flow<BluetoothScanEvent>
}

/** Creates a [BluetoothScanner] backed by `BluetoothLeScanner`. */
fun BluetoothScanner(
    context: Context,
    config: BluetoothScanConfig = BluetoothScanConfig()
): BluetoothScanner = AndroidBluetoothScanner(context, config)
