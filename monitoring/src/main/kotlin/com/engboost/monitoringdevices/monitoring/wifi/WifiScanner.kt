package com.engboost.monitoringdevices.monitoring.wifi

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
 * Periodically requests Wi-Fi scans and reports results.
 *
 * Results come from `SCAN_RESULTS_AVAILABLE_ACTION`, which since Android 10 also fires for scans
 * requested by the system or other apps. When Android rejects a request, cached results are reported.
 */
internal class WifiScanner(context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val availability = WifiScanAvailability(appContext, wifiManager)

    val requiredPermissions: Set<String>
        get() = availability.requiredPermissions

    val isScanThrottlingEnabled: Boolean
        get() = availability.isScanThrottlingEnabled

    /**
     * Returns a cold flow that requests a scan, waits [refreshIntervalMillis] and repeats.
     *
     * [refreshIntervalMillis] is invoked before every wait, so the interval can change while scanning.
     */
    @SuppressLint("MissingPermission")
    fun scan(refreshIntervalMillis: () -> Long): Flow<WifiScanEvent> = callbackFlow {
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
                delay(refreshIntervalMillis())
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
    private fun ProducerScope<WifiScanEvent>.readNetworks(): List<WifiNetwork>? {
        return try {
            wifiManager.scanResults.map { result -> result.toWifiNetwork() }
        } catch (error: Exception) {
            trySend(error.toScanFailureEvent("Failed to read Wi-Fi scan results"))
            close()
            null
        }
    }

    private fun ScanResult.toWifiNetwork(): WifiNetwork {
        return WifiNetwork(
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
            wifiSsid?.toString()
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
