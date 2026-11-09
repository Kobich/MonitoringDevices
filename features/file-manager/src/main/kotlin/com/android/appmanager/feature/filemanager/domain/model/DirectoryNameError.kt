package com.android.appmanager.feature.filemanager.domain.model

internal enum class DirectoryNameError {
    EMPTY,
    RESERVED,
    INVALID_CHARACTERS,
    TOO_LONG,
    ALREADY_EXISTS
}
