package com.engboost.monitoringdevices.monitoring.ui.ble

import com.engboost.monitoringdevices.monitoring.ble.BleBondState
import com.engboost.monitoringdevices.monitoring.ble.BleDevice
import com.engboost.monitoringdevices.monitoring.ble.BleDeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BleDeviceUiMapperTest {
    private val mapper = BleDeviceUiMapper()

    @Test
    fun `stable mode keeps current order and appends new devices`() {
        val current = mapper.map(listOf(device("A"), device("B")), BleSortMode.STABLE, emptyList())

        val updated = mapper.map(listOf(device("C"), device("B"), device("A")), BleSortMode.STABLE, current)

        assertEquals(listOf("A", "B", "C"), updated.map { it.address })
    }

    @Test
    fun `signal mode ignores small rssi differences within a bucket`() {
        val current = mapper.map(
            listOf(device("A", rssi = -61), device("B", rssi = -63)),
            BleSortMode.SIGNAL,
            emptyList()
        )

        // -64 and -61 are in the same 5 dBm bucket, so the previous order is kept.
        val updated = mapper.map(
            listOf(device("A", rssi = -64), device("B", rssi = -61)),
            BleSortMode.SIGNAL,
            current
        )

        assertEquals(listOf("A", "B"), updated.map { it.address })
    }

    @Test
    fun `signal mode moves much stronger device up`() {
        val current = mapper.map(
            listOf(device("A", rssi = -60), device("B", rssi = -80)),
            BleSortMode.SIGNAL,
            emptyList()
        )

        val updated = mapper.map(
            listOf(device("A", rssi = -80), device("B", rssi = -50)),
            BleSortMode.SIGNAL,
            current
        )

        assertEquals(listOf("B", "A"), updated.map { it.address })
    }

    @Test
    fun `name mode puts unnamed devices last`() {
        val devices = mapper.map(
            listOf(device("A", name = null), device("B", name = "beta"), device("C", name = "Alpha")),
            BleSortMode.NAME,
            emptyList()
        )

        assertEquals(listOf("C", "B", "A"), devices.map { it.address })
    }

    @Test
    fun `unchanged device keeps the same instance`() {
        val current = mapper.map(listOf(device("A")), BleSortMode.STABLE, emptyList())

        val updated = mapper.map(listOf(device("A")), BleSortMode.STABLE, current)

        assertSame(current.single(), updated.single())
    }

    private fun device(
        address: String,
        name: String? = "Device $address",
        rssi: Int = -60
    ) = BleDevice(
        name = name,
        address = address,
        rssiDbm = rssi,
        type = BleDeviceType.LE,
        bondState = BleBondState.NONE,
        lastSeenAgoMillis = 0L,
        isStale = false
    )
}
