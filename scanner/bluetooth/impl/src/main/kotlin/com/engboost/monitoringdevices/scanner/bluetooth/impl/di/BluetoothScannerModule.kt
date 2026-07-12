package com.engboost.monitoringdevices.scanner.bluetooth.impl.di

import com.engboost.monitoringdevices.scanner.bluetooth.api.BluetoothScanner
import com.engboost.monitoringdevices.scanner.bluetooth.impl.AndroidBluetoothScanner
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val bluetoothScannerModule = module {
    single<BluetoothScanner> { AndroidBluetoothScanner(androidContext()) }
}
