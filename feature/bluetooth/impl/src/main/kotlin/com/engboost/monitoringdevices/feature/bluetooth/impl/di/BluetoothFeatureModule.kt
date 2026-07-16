package com.engboost.monitoringdevices.feature.bluetooth.impl.di

import com.engboost.monitoringdevices.feature.bluetooth.api.BluetoothFeatureApi
import com.engboost.monitoringdevices.feature.bluetooth.impl.BluetoothFeatureImpl
import com.engboost.monitoringdevices.feature.bluetooth.impl.domain.interactor.BluetoothScanInteractor
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.BluetoothViewModel
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.mapper.BluetoothDeviceUiMapper
import com.engboost.monitoringdevices.feature.bluetooth.impl.presentation.reducer.BluetoothUiStateReducer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bluetoothFeatureModule = module {
    single<BluetoothFeatureApi> { BluetoothFeatureImpl() }
    factory { BluetoothScanInteractor(get()) }
    factory { BluetoothDeviceUiMapper() }
    factory { BluetoothUiStateReducer(get()) }
    viewModel { BluetoothViewModel(get(), get()) }
}
