package com.engboost.monitoringdevices.feature.filemanager.impl.domain.model

internal enum class DirectoryNameError {
    EMPTY,
    RESERVED,
    INVALID_CHARACTERS,
    TOO_LONG,
    ALREADY_EXISTS
}
