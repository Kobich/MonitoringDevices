package com.engboost.monitoringdevices.features.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.engboost.monitoringdevices.features.ConditionFeature
import com.engboost.monitoringdevices.features.ConditionResult
import com.engboost.monitoringdevices.features.DataFeature
import com.engboost.monitoringdevices.features.Reading
import com.engboost.monitoringdevices.features.UnknownReason
import com.engboost.monitoringdevices.features.valueOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Bluetooth devices that are connected right now (not just bonded):
 * audio profiles (headset, A2DP, hearing aid, LE audio) and GATT connections.
 *
 * [getData] returns null when Bluetooth is unavailable, turned off or permissions are missing.
 * [isMeetsCondition] checks whether a device with one of the given names is connected.
 *
 * Unknown reasons: [UnknownReason.SOURCE_UNAVAILABLE], [UnknownReason.SOURCE_DISABLED],
 * [UnknownReason.PERMISSION_DENIED].
 */
@SuppressLint("MissingPermission")
class BluetoothConnectedDevicesFeature(context: Context) :
    DataFeature<List<BluetoothConnectedDevice>>,
    ConditionFeature<List<String>> {

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)

    override suspend fun getData(): List<BluetoothConnectedDevice>? = readConnectedDevices().valueOrNull()

    override suspend fun isMeetsCondition(input: List<String>): ConditionResult {
        val connected = when (val reading = readConnectedDevices()) {
            is Reading.Failure -> return ConditionResult.Unknown(reading.reason)
            is Reading.Success -> reading.value
        }

        val connectedNames = connected.mapNotNullTo(HashSet()) { it.name }
        return ConditionResult.of(input.any { it in connectedNames })
    }

    private suspend fun readConnectedDevices(): Reading<List<BluetoothConnectedDevice>> {
        val manager = bluetoothManager ?: return Reading.Failure(UnknownReason.SOURCE_UNAVAILABLE)
        val adapter = manager.adapter ?: return Reading.Failure(UnknownReason.SOURCE_UNAVAILABLE)
        return try {
            if (!adapter.isEnabled) return Reading.Failure(UnknownReason.SOURCE_DISABLED)

            val profileDevices = profiles().flatMap { readConnectedDevices(adapter, it) }
            val gattDevices = manager.getConnectedDevices(BluetoothProfile.GATT)
            val devices = (profileDevices + gattDevices)
                .distinctBy { it.address }
                .map { it.toConnectedDevice() }
            Reading.Success(devices)
        } catch (error: SecurityException) {
            Reading.Failure(UnknownReason.PERMISSION_DENIED)
        }
    }

    private fun profiles(): List<Int> = listOf(
        BluetoothProfile.HEADSET,
        BluetoothProfile.A2DP,
        BluetoothProfile.HEARING_AID,
        BluetoothProfile.LE_AUDIO
    )

    /** Profile proxy is delivered asynchronously, so wait for it with a timeout. */
    private suspend fun readConnectedDevices(
        adapter: BluetoothAdapter,
        profile: Int
    ): List<BluetoothDevice> {
        val result = withTimeoutOrNull(PROFILE_PROXY_TIMEOUT) {
            suspendCancellableCoroutine<Result<List<BluetoothDevice>>> { continuation ->
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        val devices = runCatching { proxy.connectedDevices }
                        adapter.closeProfileProxy(profile, proxy)
                        if (continuation.isActive) continuation.resume(devices)
                    }

                    override fun onServiceDisconnected(profile: Int) = Unit
                }

                if (!adapter.getProfileProxy(appContext, listener, profile)) {
                    continuation.resume(Result.success(emptyList()))
                }
            }
        }
        return result?.getOrThrow().orEmpty()
    }

    private fun BluetoothDevice.toConnectedDevice() = BluetoothConnectedDevice(
        name = name?.takeIf { it.isNotEmpty() },
        address = address
    )

    private companion object {
        val PROFILE_PROXY_TIMEOUT = 2.seconds
    }
}
