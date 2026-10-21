package com.engboost.monitoringdevices.features

/**
 * Base contract for every feature. A feature either provides data ([DataFeature])
 * or accepts input and checks a condition ([ConditionFeature]).
 */
sealed interface Feature {
    val id: String
}

/** Feature that provides data of type [T]. */
interface DataFeature<out T> : Feature {
    suspend fun getData(): FeatureResult<T>
}

/** Feature that accepts [input] and answers whether it meets the condition. */
interface ConditionFeature<in P> : Feature {
    suspend fun isMeetsCondition(input: P): ConditionResult
}

sealed interface FeatureResult<out T> {
    data class Success<out T>(val data: T) : FeatureResult<T>

    data class PermissionRequired(val permissions: Set<String>) : FeatureResult<Nothing>

    data class Unavailable(val reason: String) : FeatureResult<Nothing>

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : FeatureResult<Nothing>
}

/** Three-state answer: when the data is not enough to decide, the result is [Unknown]. */
sealed interface ConditionResult {
    data object Met : ConditionResult

    data object NotMet : ConditionResult

    data class Unknown(val reason: String) : ConditionResult

    companion object {
        fun of(value: Boolean): ConditionResult = if (value) Met else NotMet
    }
}
