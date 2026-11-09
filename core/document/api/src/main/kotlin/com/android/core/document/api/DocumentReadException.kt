package com.android.core.document.api

import java.io.IOException

/**
 * Why a document can't be read or shown. Thrown by [DocumentReader.read] and by methods of documents.
 *
 * @property reason what to tell the user; `message` has technical details for logs.
 */
class DocumentReadException(
    val reason: Reason,
    message: String? = null,
    cause: Throwable? = null
) : IOException(message ?: reason.name, cause) {

    enum class Reason {
        /** The file doesn't exist or isn't a file. */
        NOT_FOUND,

        /** The extension isn't supported, or the format needs a newer Android version. */
        UNSUPPORTED_FORMAT,

        /** The content can't be parsed. */
        CORRUPTED,

        /** The document is encrypted: read it again with a password. */
        PASSWORD_REQUIRED,

        /** The given password is wrong. */
        INVALID_PASSWORD,

        /** Not enough memory, for example for a huge image. */
        TOO_LARGE,

        /** Reading the file failed. */
        IO_ERROR
    }
}
