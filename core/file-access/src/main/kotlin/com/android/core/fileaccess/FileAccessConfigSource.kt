package com.android.core.fileaccess

import kotlinx.coroutines.flow.Flow

/**
 * Where the config comes from, e.g. the store module. The app binds its source into a set; without one there's no
 * config and everything is available:
 *
 * ```
 * @Binds @IntoSet abstract fun bindConfigSource(source: StoreConfigSource): FileAccessConfigSource
 * ```
 *
 * Screens wait for the first value before they show protected content, so emit one at once (a `StateFlow`). `null`,
 * an error in the flow and a config that didn't parse all mean no config.
 */
interface FileAccessConfigSource {
    val config: Flow<FileAccessConfig?>
}
