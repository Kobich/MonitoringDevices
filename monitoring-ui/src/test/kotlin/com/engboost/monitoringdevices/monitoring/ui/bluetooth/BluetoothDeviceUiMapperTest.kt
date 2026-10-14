package com.engboost.monitoringdevices.monitoring.ui.bluetooth

import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothBondState
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothDeviceInfo
import com.engboost.monitoringdevices.monitoring.bluetooth.BluetoothDeviceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BluetoothDeviceUiMapperTest {
    private val mapper = BluetoothDeviceUiMapper()

    @Test
    fun `stable mode keeps current order and appends new devices`() {
        val current = mapper.map(listOf(device("A"), device("B")), BluetoothSortMode.STABLE, emptyList())

        val updated = mapper.map(listOf(device("C"), device("B"), device("A")), BluetoothSortMode.STABLE, current)

        assertEquals(listOf("A", "B", "C"), updated.map { it.address })
    }

    @Test
    fun `signal mode ignores small rssi differences within a bucket`() {
        val current = mapper.map(
            listOf(device("A", rssi = -61), device("B", rssi = -63)),
            BluetoothSortMode.SIGNAL,
            emptyList()
        )

        // -64 and -61 are in the same 5 dBm bucket, so the previous order is kept.
        val updated = mapper.map(
            listOf(device("A", rssi = -64), device("B", rssi = -61)),
            BluetoothSortMode.SIGNAL,
            current
        )

        assertEquals(listOf("A", "B"), updated.map { it.address })
    }

    @Test
    fun `signal mode moves much stronger device up`() {
        val current = mapper.map(
            listOf(device("A", rssi = -60), device("B", rssi = -80)),
            BluetoothSortMode.SIGNAL,
            emptyList()
        )

        val updated = mapper.map(
            listOf(device("A", rssi = -80), device("B", rssi = -50)),
            BluetoothSortMode.SIGNAL,
            current
        )

        assertEquals(listOf("B", "A"), updated.map { it.address })
    }

    @Test
    fun `name mode puts unnamed devices last`() {
        val devices = mapper.map(
            listOf(device("A", name = null), device("B", name = "beta"), device("C", name = "Alpha")),
            BluetoothSortMode.NAME,
            emptyList()
        )

        assertEquals(listOf("C", "B", "A"), devices.map { it.address })
    }

    @Test
    fun `unchanged device keeps the same instance`() {
        val current = mapper.map(listOf(device("A")), BluetoothSortMode.STABLE, emptyList())

        val updated = mapper.map(listOf(device("A")), BluetoothSortMode.STABLE, current)

        assertSame(current.single(), updated.single())
    }

    private fun device(
        address: String,
        name: String? = "Device $address",
        rssi: Int = -60
    ) = BluetoothDeviceInfo(
        name = name,
        address = address,
        rssiDbm = rssi,
        type = BluetoothDeviceType.LE,
        bondState = BluetoothBondState.NONE,
        lastSeenAgoMillis = 0L,
        isStale = false
    )
}
