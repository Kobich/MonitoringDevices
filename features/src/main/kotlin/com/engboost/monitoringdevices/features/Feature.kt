package com.engboost.monitoringdevices.features

/** Feature that provides data. Returns null when the data cannot be obtained. */
interface DataFeature<out T> {
    suspend fun getData(): T?
}

/** Feature that accepts [input] and checks whether it meets the condition. */
interface ConditionFeature<in P> {
    suspend fun isMeetsCondition(input: P): ConditionResult
}

enum class ConditionResult {
    TRUE,
    FALSE,

    /** Not enough data to say for sure. */
    UNKNOWN
}
