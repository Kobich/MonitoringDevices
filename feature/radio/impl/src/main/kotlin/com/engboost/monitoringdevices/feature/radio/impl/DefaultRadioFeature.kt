package com.engboost.monitoringdevices.feature.radio.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.engboost.monitoringdevices.feature.radio.api.RadioFeatureApi
import com.engboost.monitoringdevices.feature.radio.impl.presentation.RadioRoute

class DefaultRadioFeature : RadioFeatureApi {
    @Composable
    override fun RadioScreen(modifier: Modifier) {
        RadioRoute(modifier = modifier)
    }
}
