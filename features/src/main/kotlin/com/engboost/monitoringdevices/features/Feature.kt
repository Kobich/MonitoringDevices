package com.engboost.monitoringdevices.features

/** Feature that provides data. Returns null when the data cannot be obtained. */
interface DataFeature<out T> {
    suspend fun getData(): T?
}

/** Feature that accepts [input] and checks whether it meets the condition. */
interface ConditionFeature<in P> {
    suspend fun isMeetsCondition(input: P): ConditionResult
}

sealed interface ConditionResult {
    data object True : ConditionResult

    data object False : ConditionResult

    /** Cannot say for sure; [reason] tells why. */
    data class Unknown(val reason: UnknownReason) : ConditionResult

    companion object {
        fun of(value: Boolean): ConditionResult = if (value) True else False
    }
}

/** Why a feature returned [ConditionResult.Unknown]. One list for all features. */
enum class UnknownReason {
    /** A required runtime permission is not granted. */
    PERMISSION_DENIED,

    /** The source is turned off: Wi-Fi, Bluetooth or location. */
    SOURCE_DISABLED,

    /** The device has no such hardware or system service. */
    SOURCE_UNAVAILABLE,

    /** The previous scan started too recently; Android limits how often scans start. */
    TOO_FREQUENT,

    /** The system reported an error while reading. */
    READ_FAILED,

    /** Nothing to decide on: an empty Wi-Fi cache, no location received in time. */
    NO_DATA,

    /** Only mock locations were received; they are ignored. */
    MOCK_DATA,

    /** The data is not accurate enough: the location is too close to the zone border. */
    NOT_ACCURATE_ENOUGH,

    /** Independent sources contradict each other: GPS and network location disagree, possible spoofing. */
    SOURCES_CONFLICT,

    /**
     * Only a source that cannot be trusted answered: GPS while network location did not arrive
     * in time, or the system clock set manually.
     */
    NOT_VERIFIED
}

/** Data or the reason it could not be read. Lets a feature keep the reason for [ConditionResult.Unknown]. */
internal sealed interface Reading<out T> {
    data class Success<out T>(val value: T) : Reading<T>

    data class Failure(val reason: UnknownReason) : Reading<Nothing>
}

internal fun <T> Reading<T>.valueOrNull(): T? = (this as? Reading.Success)?.value
