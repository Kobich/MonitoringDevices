package com.engboost.monitoringdevices.feature.wifi.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.feature.wifi.api.WifiFeatureApi
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.WifiFeatureScreen

internal class WifiFeatureImpl : WifiFeatureApi {
    @Composable
    override fun WifiScreen(modifier: Modifier) {
        WifiFeatureScreen(modifier = modifier)
    }
}
