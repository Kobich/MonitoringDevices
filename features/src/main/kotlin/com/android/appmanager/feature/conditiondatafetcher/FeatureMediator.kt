package com.android.appmanager.feature.conditiondatafetcher

import android.content.Context
import com.android.appmanager.feature.conditiondatafetcher.bluetooth.BluetoothConnectedDevicesFeature
import com.android.appmanager.feature.conditiondatafetcher.bluetooth.NearbyBluetoothDevicesFeature
import com.android.appmanager.feature.conditiondatafetcher.location.LocationFeature
import com.android.appmanager.feature.conditiondatafetcher.time.TimeFeature
import com.android.appmanager.feature.conditiondatafetcher.wifi.WifiNetworksFeature

/**
 * One entry point over all features: routes a [ConditionInput] to its feature and reads data by [SourceId].
 * The `when`s are exhaustive, so a new input or source does not compile until it is routed here.
 */
class FeatureMediator(
    private val time: TimeFeature,
    private val geo: LocationFeature,
    private val wifiFound: WifiNetworksFeature,
    private val btFound: NearbyBluetoothDevicesFeature,
    private val btConnected: BluetoothConnectedDevicesFeature
) {
    constructor(context: Context) : this(
        time = TimeFeature(context),
        geo = LocationFeature(context),
        wifiFound = WifiNetworksFeature(context),
        btFound = NearbyBluetoothDevicesFeature(context),
        btConnected = BluetoothConnectedDevicesFeature(context)
    )

    val features: Map<SourceId, Feature> = listOf(time, geo, wifiFound, btFound, btConnected).associateBy { it.getId() }

    suspend fun getData(id: SourceId): DataType? = when (id) {
        SourceId.TIME -> time.getData()
        SourceId.GEO -> geo.getData()
        SourceId.WIFI_FOUND -> wifiFound.getData()
        SourceId.BT_FOUND -> btFound.getData()
        SourceId.BT_CONNECTED -> btConnected.getData()
    }

    suspend fun isMeetsCondition(input: ConditionInput): ConditionResult = when (input) {
        is TimeCondition -> time.isMeetsCondition(input)
        is GeoCondition -> geo.isMeetsCondition(input)
        is WifiFoundCondition -> wifiFound.isMeetsCondition(input)
        is BtFoundCondition -> btFound.isMeetsCondition(input)
        is BtConnectedCondition -> btConnected.isMeetsCondition(input)
    }
}
