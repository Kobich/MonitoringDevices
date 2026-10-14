package com.engboost.monitoringdevices.monitoring.internal.ble

import com.engboost.monitoringdevices.monitoring.BleMonitoringConfig
import com.engboost.monitoringdevices.monitoring.ble.BleBondState
import com.engboost.monitoringdevices.monitoring.ble.BleDevice
import com.engboost.monitoringdevices.monitoring.ble.BleDeviceType
import kotlin.math.abs
import kotlin.math.roundToInt

/** One advertisement as read from the Bluetooth stack. */
internal data class BleAdvertisement(
    val name: String?,
    val address: String,
    val rssiDbm: Int,
    val type: BleDeviceType,
    val bondState: BleBondState
)

/**
 * Accumulates advertisements between snapshots: smooths RSSI, tracks the last time each device
 * was seen and drops devices after [BleMonitoringConfig.retentionMillis].
 * Thread-safe; time is passed in so the class has no Android dependencies.
 */
internal class BleDeviceRegistry(
    private val config: BleMonitoringConfig
) {
    private val entries = linkedMapOf<String, Entry>()
    private var changed = false

    @Synchronized
    fun onAdvertisement(advertisement: BleAdvertisement, nowMillis: Long) {
        val entry = entries[advertisement.address]
        if (entry == null) {
            entries[advertisement.address] = Entry(
                advertisement = advertisement,
                smoothedRssi = advertisement.rssiDbm.toDouble(),
                displayedRssi = advertisement.rssiDbm,
                lastSeenMillis = nowMillis
            )
            changed = true
            return
        }

        val wasStale = nowMillis - entry.lastSeenMillis >= config.staleAfterMillis
        entry.smoothedRssi += config.rssiSmoothingFactor * (advertisement.rssiDbm - entry.smoothedRssi)
        val roundedRssi = entry.smoothedRssi.roundToInt()
        val displayedRssi = if (abs(entry.displayedRssi - roundedRssi) < config.minRssiChangeDbm) {
            entry.displayedRssi
        } else {
            roundedRssi
        }

        changed = changed ||
            wasStale ||
            displayedRssi != entry.displayedRssi ||
            advertisement.copy(rssiDbm = 0) != entry.advertisement.copy(rssiDbm = 0)
        entry.advertisement = advertisement
        entry.displayedRssi = displayedRssi
        entry.lastSeenMillis = nowMillis
    }

    /** Removes expired devices. Returns `true` while there are devices whose age must be re-published. */
    @Synchronized
    fun removeExpired(nowMillis: Long): Boolean {
        val hadDevices = entries.isNotEmpty()
        entries.values.removeAll { entry -> nowMillis - entry.lastSeenMillis >= config.retentionMillis }
        return hadDevices
    }

    /** Returns a snapshot when something changed since the previous one, or always when [force] is set. */
    @Synchronized
    fun snapshot(nowMillis: Long, force: Boolean = false): List<BleDevice>? {
        if (!force && !changed) return null
        changed = false
        return entries.values.map { entry ->
            val ageMillis = (nowMillis - entry.lastSeenMillis).coerceAtLeast(0L)
            BleDevice(
                name = entry.advertisement.name,
                address = entry.advertisement.address,
                rssiDbm = entry.displayedRssi,
                type = entry.advertisement.type,
                bondState = entry.advertisement.bondState,
                lastSeenAgoMillis = ageMillis,
                isStale = ageMillis >= config.staleAfterMillis
            )
        }
    }

    private class Entry(
        var advertisement: BleAdvertisement,
        var smoothedRssi: Double,
        var displayedRssi: Int,
        var lastSeenMillis: Long
    )
}
