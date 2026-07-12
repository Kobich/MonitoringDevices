package com.engboost.monitoringdevices.scanner.radio.impl.di

import com.engboost.monitoringdevices.scanner.radio.api.RadioScanner
import com.engboost.monitoringdevices.scanner.radio.impl.AndroidRadioScanner
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val radioScannerModule = module {
    single<RadioScanner> { AndroidRadioScanner(androidContext()) }
}
