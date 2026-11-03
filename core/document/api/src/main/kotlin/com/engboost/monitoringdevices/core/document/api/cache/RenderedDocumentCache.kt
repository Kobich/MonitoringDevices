package com.engboost.monitoringdevices.core.document.api.cache

/** Output of readers that convert documents (e.g. office files to HTML) is cached on disk. */
interface RenderedDocumentCache {
    suspend fun sizeBytes(): Long

    suspend fun clear()
}
