package com.engboost.monitoringdevices.feature.wifi.impl.di

import com.engboost.monitoringdevices.feature.wifi.api.WifiFeatureApi
import com.engboost.monitoringdevices.feature.wifi.impl.DefaultWifiFeature
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.WifiViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val wifiFeatureModule = module {
    single<WifiFeatureApi> { DefaultWifiFeature() }
    viewModel { WifiViewModel(get()) }
}
