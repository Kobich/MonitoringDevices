package com.android.appmanager.feature.conditiondatafetcher.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.android.appmanager.feature.conditiondatafetcher.BtConnectedCondition
import com.android.appmanager.feature.conditiondatafetcher.BtConnectedData
import com.android.appmanager.feature.conditiondatafetcher.ConditionFeature
import com.android.appmanager.feature.conditiondatafetcher.ConditionResult
import com.android.appmanager.feature.conditiondatafetcher.DataFeature
import com.android.appmanager.feature.conditiondatafetcher.Reading
import com.android.appmanager.feature.conditiondatafetcher.SourceId
import com.android.appmanager.feature.conditiondatafetcher.UnknownReason
import com.android.appmanager.feature.conditiondatafetcher.valueOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Devices connected right now, not just bonded. Android has no single call for this, so it asks the
 * audio profiles and GATT. HID devices (keyboards, mice) are not covered.
 */
@SuppressLint("MissingPermission")
class BluetoothConnectedDevicesFeature(context: Context) :
    DataFeature<BtConnectedData>,
    ConditionFeature<BtConnectedCondition> {

    override fun getId() = SourceId.BT_CONNECTED

    private val appContext = context.applicationContext
    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)

    override suspend fun getData(): BtConnectedData? = readConnectedDevices().valueOrNull()?.let(::BtConnectedData)

    override suspend fun isMeetsCondition(input: BtConnectedCondition): ConditionResult {
        if (input.names.isEmpty()) return ConditionResult.False

        val connected = when (val reading = readConnectedDevices()) {
            is Reading.Failure -> return ConditionResult.Unknown(reading.reason)
            is Reading.Success -> reading.value
        }

        val connectedNames = connected.mapNotNullTo(HashSet()) { it.name }
        return ConditionResult.of(input.names.any { it in connectedNames })
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
        } catch (error: ProfileReadException) {
            Reading.Failure(UnknownReason.READ_FAILED)
        }
    }

    private fun profiles(): List<Int> = listOf(
        BluetoothProfile.HEADSET,
        BluetoothProfile.A2DP,
        BluetoothProfile.HEARING_AID,
        BluetoothProfile.LE_AUDIO
    )

    // A profile that does not answer in time is treated as having no devices.
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
        return result
            ?.getOrElse { error -> throw error as? SecurityException ?: ProfileReadException(error) }
            .orEmpty()
    }

    private class ProfileReadException(cause: Throwable) : Exception(cause)

    private fun BluetoothDevice.toConnectedDevice() = BluetoothConnectedDevice(
        name = name?.takeIf { it.isNotEmpty() },
        address = address
    )

    private companion object {
        val PROFILE_PROXY_TIMEOUT = 2.seconds
    }
}
