package com.android.appmanager.feature.conditiondatafetcher

import com.android.appmanager.feature.conditiondatafetcher.bluetooth.BluetoothConnectedDevice
import com.android.appmanager.feature.conditiondatafetcher.bluetooth.NearbyBluetoothDevice
import com.android.appmanager.feature.conditiondatafetcher.location.LocationSource
import com.android.appmanager.feature.conditiondatafetcher.time.TimeSource
import com.android.appmanager.feature.conditiondatafetcher.wifi.WifiNetwork
import java.time.Instant

/** Everything [DataFeature.getData] can return. Kotlin requires all of them in this package. */
sealed interface DataType

data class TimeData(
    val instant: Instant,
    val source: TimeSource
) : DataType

data class GeoData(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val source: LocationSource,
    val ageMillis: Long
) : DataType

data class WifiFoundData(val networks: List<WifiNetwork>) : DataType

data class BtFoundData(val devices: List<NearbyBluetoothDevice>) : DataType

data class BtConnectedData(val devices: List<BluetoothConnectedDevice>) : DataType
