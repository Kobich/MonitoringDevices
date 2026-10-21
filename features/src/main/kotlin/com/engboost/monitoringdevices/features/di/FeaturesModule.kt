package com.engboost.monitoringdevices.features.di

import com.engboost.monitoringdevices.features.wifi.WifiNetworksFeature
import org.koin.dsl.module

val featuresModule = module {
    single { WifiNetworksFeature(get()) }
}
