package com.engboost.monitoringdevices.scanner.bluetooth

import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

/** Single advertisement as read from the Bluetooth stack. */
internal data class BluetoothAdvertisement(
    val name: String?,
    val address: String,
    val rssiDbm: Int,
    val type: BluetoothDeviceType,
    val bondState: BluetoothBondState,
    val manufacturerData: Map<Int, String> = emptyMap(),
    val serviceUuids: List<UUID> = emptyList()
)

/**
 * Accumulates advertisements between snapshots.
 *
 * Smooths RSSI, tracks when each device was seen last and drops devices after
 * [BluetoothScanConfig.retentionMillis]. The class is thread-safe and receives the current
 * time as a parameter, so it has no Android dependencies and is covered by JVM unit tests.
 */
internal class BluetoothDeviceRegistry(
    private val config: BluetoothScanConfig
) {
    private val entries = linkedMapOf<String, Entry>()
    private var changed = false

    @Synchronized
    fun onAdvertisement(advertisement: BluetoothAdvertisement, nowMillis: Long) {
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

    /**
     * Removes devices not seen for [BluetoothScanConfig.retentionMillis].
     *
     * Returns `true` if the registry had devices, so their ages have to be published again.
     */
    @Synchronized
    fun removeExpired(nowMillis: Long): Boolean {
        val hadDevices = entries.isNotEmpty()
        entries.values.removeAll { entry -> nowMillis - entry.lastSeenMillis >= config.retentionMillis }
        return hadDevices
    }

    /** Returns all devices if something changed since the previous snapshot or [force] is set, otherwise `null`. */
    @Synchronized
    fun snapshot(nowMillis: Long, force: Boolean = false): List<ScannedBluetoothDevice>? {
        if (!force && !changed) return null
        changed = false
        return entries.values.map { entry ->
            val ageMillis = (nowMillis - entry.lastSeenMillis).coerceAtLeast(0L)
            ScannedBluetoothDevice(
                name = entry.advertisement.name,
                address = entry.advertisement.address,
                rssiDbm = entry.displayedRssi,
                type = entry.advertisement.type,
                bondState = entry.advertisement.bondState,
                lastSeenAgoMillis = ageMillis,
                isStale = ageMillis >= config.staleAfterMillis,
                manufacturerData = entry.advertisement.manufacturerData,
                serviceUuids = entry.advertisement.serviceUuids
            )
        }
    }

    private class Entry(
        var advertisement: BluetoothAdvertisement,
        var smoothedRssi: Double,
        var displayedRssi: Int,
        var lastSeenMillis: Long
    )
}
