package com.engboost.monitoringdevices.feature.filemanager.impl.domain.model

/**
 * Path relative to the root of the app storage, segments separated by `/`.
 * The root folder is represented by an empty string.
 */
@JvmInline
internal value class StoragePath(val value: String) {
    val isRoot: Boolean
        get() = value.isEmpty()

    val segments: List<String>
        get() = if (isRoot) emptyList() else value.split(SEPARATOR)

    val name: String
        get() = segments.lastOrNull().orEmpty()

    fun parent(): StoragePath? {
        if (isRoot) return null
        return StoragePath(value.substringBeforeLast(SEPARATOR, missingDelimiterValue = ""))
    }

    fun child(name: String): StoragePath {
        return StoragePath(if (isRoot) name else "$value$SEPARATOR$name")
    }

    /** From the root to this path, inclusive. */
    fun hierarchy(): List<StoragePath> {
        return generateSequence(this) { path -> path.parent() }
            .toList()
            .asReversed()
    }

    override fun toString(): String = "/$value"

    companion object {
        const val SEPARATOR = '/'
        val ROOT = StoragePath("")
    }
}
