package com.engboost.monitoringdevices.feature.wifi.impl.di

import com.engboost.monitoringdevices.feature.wifi.api.WifiFeatureApi
import com.engboost.monitoringdevices.feature.wifi.impl.WifiFeatureImpl
import com.engboost.monitoringdevices.feature.wifi.impl.domain.interactor.WifiScanInteractor
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.WifiViewModel
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.mapper.WifiNetworkUiMapper
import com.engboost.monitoringdevices.feature.wifi.impl.presentation.reducer.WifiUiStateReducer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val wifiFeatureModule = module {
    single<WifiFeatureApi> { WifiFeatureImpl() }
    factory { WifiScanInteractor(get()) }
    factory { WifiNetworkUiMapper() }
    factory { WifiUiStateReducer(get()) }
    viewModel { WifiViewModel(get(), get()) }
}
