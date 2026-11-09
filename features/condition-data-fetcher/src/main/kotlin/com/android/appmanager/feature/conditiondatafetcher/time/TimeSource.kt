package com.android.appmanager.feature.conditiondatafetcher.time

enum class TimeSource {
    NETWORK,

    GNSS,

    /** System clock with automatic time on. Still possible to bypass. */
    SYSTEM,

    MANUAL
}
