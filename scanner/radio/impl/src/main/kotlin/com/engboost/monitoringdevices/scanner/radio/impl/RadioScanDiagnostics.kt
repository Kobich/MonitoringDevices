package com.engboost.monitoringdevices.scanner.radio.impl

import android.telephony.TelephonyManager

internal class RadioScanDiagnostics(
    private val telephonyManager: TelephonyManager
) {
    fun unavailableReason(): String? {
        return when {
            telephonyManager.phoneType == TelephonyManager.PHONE_TYPE_NONE -> {
                "Cellular radio is not available"
            }
            telephonyManager.simState == TelephonyManager.SIM_STATE_ABSENT -> {
                "SIM card is not available"
            }
            else -> null
        }
    }

    fun emptyCellInfoReason(): String {
        return "No cell info returned. This can happen on emulator, without SIM, or when modem data is unavailable."
    }
}
