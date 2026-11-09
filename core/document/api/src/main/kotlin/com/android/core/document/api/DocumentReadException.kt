package com.android.core.document.api

import java.io.IOException

class DocumentReadException(
    val reason: Reason,
    message: String? = null,
    cause: Throwable? = null
) : IOException(message ?: reason.name, cause) {

    enum class Reason {
        NOT_FOUND,
        UNSUPPORTED_FORMAT,
        CORRUPTED,
        PASSWORD_REQUIRED,
        INVALID_PASSWORD,
        TOO_LARGE,
        IO_ERROR
    }
}
