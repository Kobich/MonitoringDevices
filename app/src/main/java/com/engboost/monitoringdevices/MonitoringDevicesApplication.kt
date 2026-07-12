package com.engboost.monitoringdevices

import android.app.Application
import com.engboost.monitoringdevices.feature.bluetooth.impl.di.bluetoothFeatureModule
import com.engboost.monitoringdevices.feature.radio.impl.di.radioFeatureModule
import com.engboost.monitoringdevices.feature.wifi.impl.di.wifiFeatureModule
import com.engboost.monitoringdevices.scanner.bluetooth.impl.di.bluetoothScannerModule
import com.engboost.monitoringdevices.scanner.radio.impl.di.radioScannerModule
import com.engboost.monitoringdevices.scanner.wifi.impl.di.wifiScannerModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MonitoringDevicesApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MonitoringDevicesApplication)
            modules(
                wifiScannerModule,
                bluetoothScannerModule,
                radioScannerModule,
                wifiFeatureModule,
                bluetoothFeatureModule,
                radioFeatureModule
            )
        }
    }
}
