package com.engboost.monitoringdevices.scanner.wifi.impl

import android.annotation.SuppressLint
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import com.engboost.monitoringdevices.scanner.wifi.api.WifiNetwork
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanConfig
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanMode
import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

class AndroidWifiScanner(context: Context) : WifiScanner {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val diagnostics = WifiScanDiagnostics(appContext, wifiManager)

    override val requiredPermissions: Set<String> = buildSet {
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.CHANGE_WIFI_STATE)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
    }

    override fun scan(config: WifiScanConfig): Flow<WifiScanEvent> {
        return when (config.mode) {
            WifiScanMode.CACHED_ONLY -> cachedScan()
            WifiScanMode.ACTIVE -> activeScan()
        }
    }

    private fun cachedScan(): Flow<WifiScanEvent> = flow {
        val missingPermissions = missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            emit(WifiScanEvent.PermissionRequired(missingPermissions))
            return@flow
        }

        diagnostics.unavailableReason()?.let { reason ->
            emit(WifiScanEvent.Unavailable(reason))
            return@flow
        }

        emit(WifiScanEvent.Networks(readScanResults()))
    }

    @SuppressLint("MissingPermission")
    private fun activeScan(): Flow<WifiScanEvent> = callbackFlow {
        val missingPermissions = missingPermissions()
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
        sendCurrentNetworks()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(WifiScanEvent.Networks(readScanResults()))
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }

        @Suppress("DEPRECATION")
        val startResult = runCatching { wifiManager.startScan() }

        startResult
            .onSuccess { isStarted ->
                if (!isStarted) {
                    val hasCachedResults = readScanResults().isNotEmpty()
                    trySend(WifiScanEvent.Unavailable(diagnostics.activeScanRejectedReason(hasCachedResults)))
                    if (!hasCachedResults) close()
                }
            }
            .onFailure { error ->
                trySend(
                    WifiScanEvent.Error(
                        message = error.toWifiScanMessage(),
                        cause = error
                    )
                )
                close(error)
            }

        awaitClose {
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    private fun missingPermissions(): Set<String> {
        return requiredPermissions
            .filter { permission ->
                appContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            }
            .toSet()
    }

    @SuppressLint("MissingPermission")
    private fun readScanResults(): List<WifiNetwork> {
        return runCatching {
            wifiManager.scanResults.map { result ->
                WifiNetwork(
                    ssid = result.readSsid(),
                    bssid = result.BSSID,
                    rssiDbm = result.level,
                    frequencyMhz = result.frequency,
                    capabilities = result.capabilities
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun ProducerScope<WifiScanEvent>.sendCurrentNetworks() {
        trySend(WifiScanEvent.Networks(readScanResults()))
    }

    private fun Throwable.toWifiScanMessage(): String {
        val details = message?.takeIf { it.isNotBlank() } ?: this::class.java.simpleName
        return "Failed to start Wi-Fi scan: $details"
    }

    private fun android.net.wifi.ScanResult.readSsid(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wifiSsid?.toString()?.takeIf { it.isNotBlank() }
        } else {
            @Suppress("DEPRECATION")
            SSID.takeIf { it.isNotBlank() }
        }
    }
}
