package com.android.core.fileaccess

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import javax.inject.Inject
import javax.inject.Singleton

/** The current config for the features: changes while they're open, [FileAccessConfig.Unrestricted] when there's none. */
@Singleton
class FileAccessRules @Inject constructor(
    sources: Set<@JvmSuppressWildcards FileAccessConfigSource>
) {
    init {
        check(sources.size <= 1) { "More than one FileAccessConfigSource is bound: $sources" }
    }

    /** After an error in the source: no config, and the source is subscribed again, so later configs still come. */
    val config: Flow<FileAccessConfig> = sources.singleOrNull()?.config
        ?.map { config -> config ?: FileAccessConfig.Unrestricted }
        ?.retryWhen { _, _ ->
            emit(FileAccessConfig.Unrestricted)
            delay(RETRY_DELAY_MILLIS)
            true
        }
        ?.distinctUntilChanged()
        ?: flowOf(FileAccessConfig.Unrestricted)

    private companion object {
        const val RETRY_DELAY_MILLIS = 5_000L
    }
}
