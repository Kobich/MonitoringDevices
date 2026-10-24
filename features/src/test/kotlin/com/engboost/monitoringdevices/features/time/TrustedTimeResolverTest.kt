package com.engboost.monitoringdevices.features.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class TrustedTimeResolverTest {
    @Test
    fun `network time comes first`() {
        val time = resolver(network = NETWORK, gnss = GNSS, isAutoTime = true).resolve()

        assertEquals(TrustedTime(Instant.ofEpochMilli(NETWORK), TimeSource.NETWORK), time)
    }

    @Test
    fun `satellite time when there is no network time`() {
        val time = resolver(network = null, gnss = GNSS, isAutoTime = true).resolve()

        assertEquals(TrustedTime(Instant.ofEpochMilli(GNSS), TimeSource.GNSS), time)
    }

    @Test
    fun `system time when automatic time is on`() {
        val time = resolver(network = null, gnss = null, isAutoTime = true).resolve()

        assertEquals(TrustedTime(Instant.ofEpochMilli(SYSTEM), TimeSource.SYSTEM), time)
    }

    @Test
    fun `no trusted time when the time is set manually`() {
        assertNull(resolver(network = null, gnss = null, isAutoTime = false).resolve())
    }

    @Test
    fun `manual time does not matter while network time is known`() {
        val time = resolver(network = NETWORK, gnss = null, isAutoTime = false).resolve()

        assertEquals(TimeSource.NETWORK, time?.source)
    }

    private fun resolver(network: Long?, gnss: Long?, isAutoTime: Boolean) = TrustedTimeResolver(
        networkTimeMillis = { network },
        gnssTimeMillis = { gnss },
        systemTimeMillis = { SYSTEM },
        isAutoTimeEnabled = { isAutoTime }
    )

    private companion object {
        const val NETWORK = 1_000L
        const val GNSS = 2_000L
        const val SYSTEM = 3_000L
    }
}
