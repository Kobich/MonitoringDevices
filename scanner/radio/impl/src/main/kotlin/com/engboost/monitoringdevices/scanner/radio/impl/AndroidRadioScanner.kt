@file:Suppress("DEPRECATION")

package com.engboost.monitoringdevices.scanner.radio.impl

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import com.engboost.monitoringdevices.scanner.radio.api.RadioCellInfo
import com.engboost.monitoringdevices.scanner.radio.api.RadioNetworkType
import com.engboost.monitoringdevices.scanner.radio.api.RadioScanConfig
import com.engboost.monitoringdevices.scanner.radio.api.RadioScanEvent
import com.engboost.monitoringdevices.scanner.radio.api.RadioScanMode
import com.engboost.monitoringdevices.scanner.radio.api.RadioScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AndroidRadioScanner(context: Context) : RadioScanner {
    private val appContext = context.applicationContext
    private val telephonyManager = appContext.getSystemService(TelephonyManager::class.java)
    private val diagnostics = RadioScanDiagnostics(telephonyManager)

    override val requiredPermissions: Set<String> = setOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_PHONE_STATE
    )

    @SuppressLint("MissingPermission")
    override fun scan(config: RadioScanConfig): Flow<RadioScanEvent> = flow {
        val missingPermissions = missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            emit(RadioScanEvent.PermissionRequired(missingPermissions))
            return@flow
        }

        diagnostics.unavailableReason()?.let { reason ->
            emit(RadioScanEvent.Unavailable(reason))
            return@flow
        }

        emit(RadioScanEvent.Scanning)

        val cells = runCatching {
            telephonyManager.allCellInfo
                .orEmpty()
                .filter { cell ->
                    config.mode == RadioScanMode.ALL_AVAILABLE || cell.isRegistered
                }
                .map { cell -> cell.toRadioCellInfo() }
        }.getOrElse { error ->
            emit(RadioScanEvent.Error("Failed to read radio cell info", error))
            return@flow
        }

        if (cells.isEmpty()) {
            emit(RadioScanEvent.Unavailable(diagnostics.emptyCellInfoReason()))
            return@flow
        }

        emit(RadioScanEvent.Cells(cells))
    }

    private fun missingPermissions(): Set<String> {
        return requiredPermissions
            .filter { permission ->
                appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            }
            .toSet()
    }

    private fun CellInfo.toRadioCellInfo(): RadioCellInfo {
        return RadioCellInfo(
            networkType = toRadioNetworkType(),
            isRegistered = isRegistered,
            signalStrengthDbm = signalStrengthDbm(),
            operatorName = telephonyManager.networkOperatorName.takeIf { it.isNotBlank() }
        )
    }

    private fun CellInfo.toRadioNetworkType(): RadioNetworkType {
        return when {
            this is CellInfoGsm -> RadioNetworkType.GSM
            isCdmaCellInfo() -> RadioNetworkType.CDMA
            this is CellInfoWcdma -> RadioNetworkType.WCDMA
            this is CellInfoLte -> RadioNetworkType.LTE
            isNrCellInfo() -> RadioNetworkType.NR
            else -> RadioNetworkType.UNKNOWN
        }
    }

    private fun CellInfo.signalStrengthDbm(): Int? {
        return when {
            this is CellInfoGsm -> cellSignalStrength.dbm
            isCdmaCellInfo() -> readCdmaSignalStrengthDbm()
            this is CellInfoWcdma -> cellSignalStrength.dbm
            this is CellInfoLte -> cellSignalStrength.dbm
            isNrCellInfo() -> readNrSignalStrengthDbm()
            else -> null
        }?.takeUnless { it == CellInfo.UNAVAILABLE }
    }

    private fun CellInfo.isCdmaCellInfo(): Boolean = this is CellInfoCdma

    private fun CellInfo.isNrCellInfo(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && this is CellInfoNr
    }

    private fun CellInfo.readCdmaSignalStrengthDbm(): Int? {
        return (this as? CellInfoCdma)?.cellSignalStrength?.dbm
    }

    private fun CellInfo.readNrSignalStrengthDbm(): Int? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (this as? CellInfoNr)?.cellSignalStrength?.dbm
        } else {
            null
        }
    }
}
