package com.engboost.monitoringdevices

import android.app.Application
import com.engboost.monitoringdevices.scanner.NearbyScanner

class MonitoringDevicesApplication : Application() {
    val scanner: NearbyScanner by lazy { NearbyScanner(this) }
}
