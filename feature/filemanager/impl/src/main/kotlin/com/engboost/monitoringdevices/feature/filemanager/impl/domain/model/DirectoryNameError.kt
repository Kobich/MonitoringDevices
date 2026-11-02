package com.engboost.monitoringdevices.feature.filemanager.impl.domain.model

internal enum class DirectoryNameError {
    EMPTY,
    RESERVED,
    INVALID_CHARACTERS,
    TOO_LONG,
    ALREADY_EXISTS
}

internal sealed interface CreateDirectoryResult {
    data class Created(val entry: FileEntry) : CreateDirectoryResult
    data class Invalid(val error: DirectoryNameError) : CreateDirectoryResult
}
