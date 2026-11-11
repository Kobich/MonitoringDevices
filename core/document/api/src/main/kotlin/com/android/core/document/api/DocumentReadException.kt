package com.android.core.document.api

import java.io.IOException

/** Also thrown by the methods of documents. [reason] is for the user, `message` for logs. */
class DocumentReadException(
    val reason: Reason,
    message: String? = null,
    cause: Throwable? = null
) : IOException(message ?: reason.name, cause) {

    enum class Reason {
        NOT_FOUND,

        /** Also when the format needs a newer Android version, or the file is encrypted. */
        UNSUPPORTED_FORMAT,

        CORRUPTED,

        /** Not enough memory, e.g. for a huge image; not about the file size. */
        TOO_LARGE,

        IO_ERROR
    }
}
