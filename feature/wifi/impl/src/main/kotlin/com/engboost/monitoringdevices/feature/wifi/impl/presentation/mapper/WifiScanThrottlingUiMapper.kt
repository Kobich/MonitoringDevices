package com.engboost.monitoringdevices.feature.wifi.impl.presentation.mapper

import com.engboost.monitoringdevices.feature.wifi.impl.domain.model.WifiScanThrottling
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.model.WifiScanThrottlingHintUi

internal class WifiScanThrottlingUiMapper {
    fun mapDialog(status: WifiScanThrottling): WifiScanThrottlingHintUi? {
        return when (status.isEnabled) {
            true -> WifiScanThrottlingHintUi(
                title = "Wi-Fi scan throttling is enabled",
                message = "Android limits Wi-Fi scan frequency. Current refresh interval is 30 seconds. For local testing you can disable Wi-Fi scan throttling in Developer options.",
                actionText = "Open Developer options"
            )

            false -> null

            null -> WifiScanThrottlingHintUi(
                title = "Wi-Fi scan throttling status unknown",
                message = "Android setting is not readable on this device. Safe refresh interval is 30 seconds.",
                actionText = "Open Developer options"
            )
        }
    }
}
