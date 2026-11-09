package com.android.appmanager.feature.conditiondatafetcher

enum class SourceId {
    TIME,
    GEO,
    WIFI_FOUND,
    BT_FOUND,
    BT_CONNECTED
}

/** Common parent, so the data and the condition side of one feature share the same id. */
interface Feature {
    fun getId(): SourceId
}

/** Returns null when the data cannot be obtained. */
interface DataFeature<out T : DataType> : Feature {
    suspend fun getData(): T?
}

interface ConditionFeature<in P : ConditionInput> : Feature {
    suspend fun isMeetsCondition(input: P): ConditionResult
}

sealed interface ConditionResult {
    data object True : ConditionResult

    data object False : ConditionResult

    data class Unknown(val reason: UnknownReason) : ConditionResult

    companion object {
        fun of(value: Boolean): ConditionResult = if (value) True else False
    }
}

/** One list for all features. Which feature reports which reason: see README. */
enum class UnknownReason {
    PERMISSION_DENIED,

    SOURCE_DISABLED,

    SOURCE_UNAVAILABLE,

    /** Android limits how often scans can start. */
    TOO_FREQUENT,

    READ_FAILED,

    NO_DATA,

    /** The user is too close to the zone border to decide. */
    NOT_ACCURATE_ENOUGH,

    /** Only the manually set clock is available. */
    NOT_VERIFIED
}

/** Keeps the failure reason inside a feature, so [ConditionResult.Unknown] can report it. */
internal sealed interface Reading<out T> {
    data class Success<out T>(val value: T) : Reading<T>

    data class Failure(val reason: UnknownReason) : Reading<Nothing>
}

internal fun <T> Reading<T>.valueOrNull(): T? = (this as? Reading.Success)?.value
