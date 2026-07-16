package com.engboost.monitoringdevices.scanner.wifi.impl

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanConfig
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanMode
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import com.engboost.monitoringdevices.scanner.wifi.api.WifiNetwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class AndroidWifiScanner(context: Context) : WifiScanner {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val diagnostics = WifiScanDiagnostics(appContext, wifiManager)
    private val permissions = WifiScanPermissions(appContext)
    private val resultReader = WifiScanResultReader(wifiManager)
    private val throttlingStatusReader = WifiScanThrottlingStatusReader(appContext)

    override val requiredPermissions: Set<String> = permissions.requiredPermissions
    override val isScanThrottlingEnabled: Boolean
        get() = throttlingStatusReader.readThrottleStatus()

    override fun scan(config: WifiScanConfig): Flow<WifiScanEvent> {
        return when (config.mode) {
            WifiScanMode.CACHED_ONLY -> cachedScan()
            WifiScanMode.ACTIVE -> activeScan(config)
        }
    }

    private fun cachedScan(): Flow<WifiScanEvent> = flow {
        val missingPermissions = permissions.missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            emit(WifiScanEvent.PermissionRequired(missingPermissions))
            return@flow
        }

        diagnostics.unavailableReason()?.let { reason ->
            emit(WifiScanEvent.Unavailable(reason))
            return@flow
        }

        val networks = try {
            resultReader.readScanResults()
        } catch (error: Exception) {
            emit(error.toScanFailureEvent("Failed to read Wi-Fi scan results"))
            return@flow
        }

        emit(WifiScanEvent.Networks(networks))
    }

    @SuppressLint("MissingPermission")
    private fun activeScan(config: WifiScanConfig): Flow<WifiScanEvent> = callbackFlow {
        val missingPermissions = permissions.missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            trySend(WifiScanEvent.PermissionRequired(missingPermissions))
            close()
            return@callbackFlow
        }

        diagnostics.unavailableReason()?.let { reason ->
            trySend(WifiScanEvent.Unavailable(reason))
            close()
            return@callbackFlow
        }

        trySend(WifiScanEvent.Scanning)
        if (!sendCurrentNetworks()) return@callbackFlow

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                    sendCurrentNetworks()
                }
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }

        val activeScanJob = launch {
            while (true) {
                requestActiveScan()

                val refreshIntervalMillis = config.refreshIntervalMillis ?: break
                delay(refreshIntervalMillis)
            }
        }

        awaitClose {
            activeScanJob.cancel()
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun ProducerScope<WifiScanEvent>.requestActiveScan() {
        @Suppress("DEPRECATION")
        val startResult = runCatching { wifiManager.startScan() }

        startResult
            .onSuccess { isStarted ->
                if (!isStarted) {
                    val cachedNetworks = readCurrentNetworks() ?: return@onSuccess
                    if (cachedNetworks.isNotEmpty()) {
                        trySend(WifiScanEvent.Networks(cachedNetworks))
                    } else {
                        trySend(WifiScanEvent.Unavailable(diagnostics.activeScanRejectedReason()))
                        close()
                    }
                }
            }
            .onFailure { error ->
                trySend(error.toScanFailureEvent("Failed to start Wi-Fi scan"))
                close()
            }
    }

    private fun ProducerScope<WifiScanEvent>.sendCurrentNetworks(): Boolean {
        val networks = readCurrentNetworks() ?: return false
        trySend(WifiScanEvent.Networks(networks))
        return true
    }

    private fun ProducerScope<WifiScanEvent>.readCurrentNetworks(): List<WifiNetwork>? {
        return try {
            resultReader.readScanResults()
        } catch (error: Exception) {
            trySend(error.toScanFailureEvent("Failed to read Wi-Fi scan results"))
            close()
            null
        }
    }

    private fun Throwable.toScanFailureEvent(action: String): WifiScanEvent {
        val missingPermissions = permissions.missingPermissions()
        if (this is SecurityException && missingPermissions.isNotEmpty()) {
            return WifiScanEvent.PermissionRequired(missingPermissions)
        }

        val details = message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
        return WifiScanEvent.Error(
            message = "$action: $details",
            cause = this
        )
    }
}
