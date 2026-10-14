package com.engboost.monitoringdevices.scanner.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothDeviceRegistryTest {
    private val config = BluetoothScanConfig(
        staleAfterMillis = 30_000L,
        retentionMillis = 120_000L,
        rssiSmoothingFactor = 0.25,
        minRssiChangeDbm = 3
    )
    private val registry = BluetoothDeviceRegistry(config)

    @Test
    fun `new device produces snapshot with raw rssi`() {
        registry.onAdvertisement(advertisement(rssi = -60), nowMillis = 0L)

        val devices = registry.snapshot(nowMillis = 100L)!!

        assertEquals(1, devices.size)
        assertEquals(-60, devices.single().rssiDbm)
        assertEquals(100L, devices.single().lastSeenAgoMillis)
        assertFalse(devices.single().isStale)
    }

    @Test
    fun `snapshot is skipped when nothing changed`() {
        registry.onAdvertisement(advertisement(rssi = -60), nowMillis = 0L)
        registry.snapshot(nowMillis = 0L)

        registry.onAdvertisement(advertisement(rssi = -61), nowMillis = 10L)

        assertNull(registry.snapshot(nowMillis = 10L))
        assertNotNull(registry.snapshot(nowMillis = 10L, force = true))
    }

    @Test
    fun `rssi is smoothed and small changes are suppressed`() {
        registry.onAdvertisement(advertisement(rssi = -60), nowMillis = 0L)
        registry.snapshot(nowMillis = 0L)

        // -60 + 0.25 * (-80 - -60) = -65: change of 5 dBm is reported.
        registry.onAdvertisement(advertisement(rssi = -80), nowMillis = 10L)
        assertEquals(-65, registry.snapshot(nowMillis = 10L)!!.single().rssiDbm)

        // -65 + 0.25 * (-68 - -65) = -65.75 -> -66: change of 1 dBm is suppressed.
        registry.onAdvertisement(advertisement(rssi = -68), nowMillis = 20L)
        assertNull(registry.snapshot(nowMillis = 20L))
    }

    @Test
    fun `name change is reported even with stable rssi`() {
        registry.onAdvertisement(advertisement(name = null), nowMillis = 0L)
        registry.snapshot(nowMillis = 0L)

        registry.onAdvertisement(advertisement(name = "Tag"), nowMillis = 10L)

        assertEquals("Tag", registry.snapshot(nowMillis = 10L)!!.single().name)
    }

    @Test
    fun `device becomes stale and fresh again`() {
        registry.onAdvertisement(advertisement(), nowMillis = 0L)
        registry.snapshot(nowMillis = 0L)

        assertTrue(registry.snapshot(nowMillis = 30_000L, force = true)!!.single().isStale)

        registry.onAdvertisement(advertisement(), nowMillis = 31_000L)
        val devices = registry.snapshot(nowMillis = 31_000L)!!
        assertFalse(devices.single().isStale)
    }

    @Test
    fun `expired devices are removed`() {
        registry.onAdvertisement(advertisement(address = "A"), nowMillis = 0L)
        registry.onAdvertisement(advertisement(address = "B"), nowMillis = 60_000L)

        assertTrue(registry.removeExpired(nowMillis = 120_000L))

        assertEquals(listOf("B"), registry.snapshot(nowMillis = 120_000L, force = true)!!.map { it.address })
    }

    @Test
    fun `empty registry does not request age refresh`() {
        assertFalse(registry.removeExpired(nowMillis = 0L))
    }

    private fun advertisement(
        address: String = "00:11:22:33:44:55",
        name: String? = "Device",
        rssi: Int = -60
    ) = BluetoothAdvertisement(
        name = name,
        address = address,
        rssiDbm = rssi,
        type = BluetoothDeviceType.LE,
        bondState = BluetoothBondState.NONE
    )
}
