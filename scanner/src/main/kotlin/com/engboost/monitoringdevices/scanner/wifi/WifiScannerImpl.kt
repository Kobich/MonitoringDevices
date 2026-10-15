package com.engboost.monitoringdevices.scanner.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [WifiScannerApi] that periodically requests scans from `WifiManager`.
 *
 * Results come from `SCAN_RESULTS_AVAILABLE_ACTION`, which since Android 10 also fires for scans
 * requested by the system or other apps. When Android rejects a request, cached results are reported.
 */
internal class WifiScannerImpl(
    context: Context,
    private val config: WifiScanConfig
) : WifiScannerApi {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val availability = WifiScanAvailability(appContext, wifiManager)

    override val requiredPermissions: Set<String>
        get() = availability.requiredPermissions

    override val isScanThrottlingEnabled: Boolean
        get() = availability.isScanThrottlingEnabled

    override fun missingPermissions(): Set<String> = availability.missingPermissions()

    @SuppressLint("MissingPermission")
    override fun scan(): Flow<WifiScanEvent> = callbackFlow {
        val missingPermissions = availability.missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            trySend(WifiScanEvent.PermissionRequired(missingPermissions))
            close()
            return@callbackFlow
        }

        availability.unavailableReason()?.let { reason ->
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

        val scanRequestJob = launch {
            while (isActive) {
                requestScan()
                // Re-read on every cycle: throttling can be toggled in Developer options at any time.
                delay(config.refreshIntervalMillis(isScanThrottlingEnabled))
            }
        }

        awaitClose {
            scanRequestJob.cancel()
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    private fun ProducerScope<WifiScanEvent>.requestScan() {
        @Suppress("DEPRECATION")
        val startResult = runCatching { wifiManager.startScan() }

        startResult
            .onSuccess { isStarted ->
                if (isStarted) return@onSuccess
                val cachedNetworks = readNetworks() ?: return@onSuccess
                if (cachedNetworks.isNotEmpty()) {
                    trySend(WifiScanEvent.Networks(cachedNetworks))
                } else {
                    trySend(WifiScanEvent.Unavailable(WifiUnavailableReason.SCAN_REJECTED))
                    close()
                }
            }
            .onFailure { error ->
                trySend(error.toScanFailureEvent("Failed to start Wi-Fi scan"))
                close()
            }
    }

    private fun ProducerScope<WifiScanEvent>.sendCurrentNetworks(): Boolean {
        val networks = readNetworks() ?: return false
        trySend(WifiScanEvent.Networks(networks))
        return true
    }

    /** Reads cached scan results; on failure sends a terminal event, closes the flow and returns `null`. */
    @SuppressLint("MissingPermission")
    private fun ProducerScope<WifiScanEvent>.readNetworks(): List<ScannedWifiNetwork>? {
        return try {
            wifiManager.scanResults.map { result -> result.toScannedWifiNetwork() }
        } catch (error: Exception) {
            trySend(error.toScanFailureEvent("Failed to read Wi-Fi scan results"))
            close()
            null
        }
    }

    private fun ScanResult.toScannedWifiNetwork(): ScannedWifiNetwork {
        return ScannedWifiNetwork(
            ssid = readSsid(),
            bssid = BSSID,
            rssiDbm = level,
            frequencyMhz = frequency,
            capabilities = capabilities
        )
    }

    @Suppress("DEPRECATION")
    private fun ScanResult.readSsid(): String? {
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // WifiSsid.toString() wraps a UTF-8 name in quotes, unlike the deprecated SSID field.
            wifiSsid?.toString()?.removeSurrounding("\"")
        } else {
            SSID
        }
        return ssid?.takeIf { it.isNotBlank() }
    }

    private fun Throwable.toScanFailureEvent(action: String): WifiScanEvent {
        val missingPermissions = availability.missingPermissions()
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
