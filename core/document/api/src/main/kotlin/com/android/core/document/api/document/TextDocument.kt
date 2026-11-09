package com.android.core.document.api.document

import com.android.core.document.api.DocumentFormat

/**
 * A plain text file.
 *
 * @property charsetName detected encoding, for example `UTF-8` or `windows-1251`.
 * @property isTruncated the file is longer than the reader reads; [text] is its beginning.
 */
data class TextDocument(
    override val name: String,
    override val format: DocumentFormat,
    val text: String,
    val charsetName: String,
    val isTruncated: Boolean
) : Document
