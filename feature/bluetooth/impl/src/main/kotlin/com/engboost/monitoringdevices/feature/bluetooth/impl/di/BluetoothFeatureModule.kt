package com.engboost.monitoringdevices.feature.bluetooth.impl.di

import com.engboost.monitoringdevices.feature.bluetooth.api.BluetoothFeatureApi
import com.engboost.monitoringdevices.feature.bluetooth.impl.DefaultBluetoothFeature
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.BluetoothViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bluetoothFeatureModule = module {
    single<BluetoothFeatureApi> { DefaultBluetoothFeature() }
    viewModel { BluetoothViewModel(get()) }
}
