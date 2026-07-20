package com.engboost.monitoringdevices.service.monitoring.impl.di

import com.engboost.monitoringdevices.service.monitoring.api.MonitoringController
import com.engboost.monitoringdevices.service.monitoring.impl.DefaultMonitoringController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val monitoringServiceModule = module {
    single { DefaultMonitoringController(androidContext(), get(), get()) }
    single<MonitoringController> { get<DefaultMonitoringController>() }
}
