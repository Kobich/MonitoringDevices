package com.engboost.monitoringdevices.scanner.wifi.impl.di

import com.engboost.monitoringdevices.scanner.wifi.api.WifiScanner
import com.engboost.monitoringdevices.scanner.wifi.impl.AndroidWifiScanner
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val wifiScannerModule = module {
    single<WifiScanner> { AndroidWifiScanner(androidContext()) }
}
