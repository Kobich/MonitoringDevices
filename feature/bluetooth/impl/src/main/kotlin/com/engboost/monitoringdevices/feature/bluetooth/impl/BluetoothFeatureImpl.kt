package com.engboost.monitoringdevices.feature.bluetooth.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.feature.bluetooth.api.BluetoothFeatureApi
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.BluetoothFeatureScreen

internal class BluetoothFeatureImpl : BluetoothFeatureApi {
    @Composable
    override fun BluetoothScreen(modifier: Modifier) {
        BluetoothFeatureScreen(modifier = modifier)
    }
}
