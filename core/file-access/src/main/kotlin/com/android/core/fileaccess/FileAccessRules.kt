package com.android.core.fileaccess

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    val config: Flow<FileAccessConfig> = sources.singleOrNull()?.config
        ?.map { config -> config ?: FileAccessConfig.Unrestricted }
        ?.catch { emit(FileAccessConfig.Unrestricted) }
        ?.distinctUntilChanged()
        ?: flowOf(FileAccessConfig.Unrestricted)
}
