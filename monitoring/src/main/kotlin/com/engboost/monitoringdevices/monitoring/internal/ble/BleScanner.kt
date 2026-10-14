package com.engboost.monitoringdevices.monitoring.internal.ble

import com.engboost.monitoringdevices.monitoring.ble.BleScanEvent
import kotlinx.coroutines.flow.Flow

internal interface BleScanner {
    val requiredPermissions: Set<String>

    fun scan(): Flow<BleScanEvent>
}
