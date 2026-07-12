package com.engboost.monitoringdevices.feature.radio.impl.di

import com.engboost.monitoringdevices.feature.radio.api.RadioFeatureApi
import com.engboost.monitoringdevices.feature.radio.impl.DefaultRadioFeature
import com.engboost.monitoringdevices.feature.radio.impl.presentation.RadioViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val radioFeatureModule = module {
    single<RadioFeatureApi> { DefaultRadioFeature() }
    viewModel { RadioViewModel(get()) }
}
