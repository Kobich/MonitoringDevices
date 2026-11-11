package com.android.core.document.impl.office

import java.io.File

/** Rewrites an office file before conversion, for what OpenDocument.core doesn't show. */
internal fun interface DocumentPatch {
    /** Writes [output] and returns `true`, or returns `false` when nothing needed a change. */
    fun apply(input: File, output: File): Boolean
}
