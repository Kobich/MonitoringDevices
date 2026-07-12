package com.engboost.monitoringdevices.feature.bluetooth.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.feature.bluetooth.api.BluetoothFeatureApi
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.BluetoothRoute

class DefaultBluetoothFeature : BluetoothFeatureApi {
    @Composable
    override fun BluetoothScreen(modifier: Modifier) {
        BluetoothRoute(modifier = modifier)
    }
}
