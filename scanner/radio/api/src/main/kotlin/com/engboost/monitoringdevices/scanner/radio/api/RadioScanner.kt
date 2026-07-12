package com.engboost.monitoringdevices.scanner.radio.api

import kotlinx.coroutines.flow.Flow

interface RadioScanner {
    val requiredPermissions: Set<String>

    fun scan(config: RadioScanConfig = RadioScanConfig()): Flow<RadioScanEvent>
}

data class RadioScanConfig(
    val mode: RadioScanMode = RadioScanMode.ALL_AVAILABLE
)

enum class RadioScanMode {
    REGISTERED_ONLY,
    ALL_AVAILABLE
}

sealed interface RadioScanEvent {
    data object Scanning : RadioScanEvent

    data class Cells(
        val cells: List<RadioCellInfo>
    ) : RadioScanEvent

    data class PermissionRequired(
        val permissions: Set<String>
    ) : RadioScanEvent

    data class Unavailable(
        val reason: String
    ) : RadioScanEvent

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : RadioScanEvent
}

data class RadioCellInfo(
    val networkType: RadioNetworkType,
    val isRegistered: Boolean,
    val signalStrengthDbm: Int?,
    val operatorName: String?
)

enum class RadioNetworkType {
    GSM,
    CDMA,
    WCDMA,
    LTE,
    NR,
    UNKNOWN
}
