package com.engboost.monitoringdevices.scanner.ui.wifi

import com.engboost.monitoringdevices.scanner.ui.common.ScanStatus
import com.engboost.monitoringdevices.scanner.wifi.ScannedWifiNetwork
import com.engboost.monitoringdevices.scanner.wifi.WifiScanEvent
import com.engboost.monitoringdevices.scanner.wifi.WifiUnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiUiStateReducerTest {
    private val scanning = WifiUiState(status = ScanStatus.Scanning)

    @Test
    fun `networks are sorted by signal and hidden networks get a name`() {
        val networks = listOf(
            network("A", ssid = "Home", rssi = -70),
            network("B", ssid = null, rssi = -40)
        )

        val state = scanning.reduce(WifiScanEvent.Networks(networks))

        assertEquals(listOf("B", "A"), state.networks.map { it.bssid })
        assertEquals("Hidden network", state.networks.first().ssid)
        assertTrue(state.isScanning)
    }

    @Test
    fun `terminal event stops scanning`() {
        val state = scanning
            .reduce(WifiScanEvent.Networks(listOf(network("A"))))
            .reduce(WifiScanEvent.Unavailable(WifiUnavailableReason.WIFI_DISABLED))

        assertEquals(ScanStatus.Unavailable(WifiUnavailableReason.WIFI_DISABLED), state.status)
        assertTrue(state.networks.isEmpty())
        assertFalse(state.isScanning)
    }

    @Test
    fun `error keeps networks and reports the cause`() {
        val state = scanning
            .reduce(WifiScanEvent.Networks(listOf(network("A"))))
            .reduce(WifiScanEvent.Error("Scan failed", IllegalStateException("busy")))

        assertEquals(ScanStatus.Failed("Scan failed: busy"), state.status)
        assertEquals(1, state.networks.size)
    }

    @Test
    fun `selected network disappears with its access point`() {
        val withSelection = scanning
            .reduce(WifiScanEvent.Networks(listOf(network("A"))))
            .let { it.copy(selectedNetwork = it.networks.single()) }

        val state = withSelection.reduce(WifiScanEvent.Networks(listOf(network("B"))))

        assertNull(state.selectedNetwork)
    }

    @Test
    fun `every permission request gets a new id`() {
        val first = scanning.reduce(WifiScanEvent.PermissionRequired(setOf("p")))
        val second = first.reduce(WifiScanEvent.PermissionRequired(setOf("p")))

        assertEquals(ScanStatus.PermissionsRequired, second.status)
        assertEquals(first.permissionRequest!!.id + 1, second.permissionRequest!!.id)
    }

    private fun network(
        bssid: String,
        ssid: String? = "Network $bssid",
        rssi: Int = -60
    ) = ScannedWifiNetwork(
        ssid = ssid,
        bssid = bssid,
        rssiDbm = rssi,
        frequencyMhz = 2412,
        capabilities = "[WPA2-PSK-CCMP]"
    )
}
