package com.android.core.document.impl.office

import java.io.File

/** Rewrites an office file before conversion, for what OpenDocument.core doesn't show. */
internal fun interface DocumentPatch {
    /** @return `false` if nothing needed a change; [output] is written only when `true`. */
    fun apply(input: File, output: File): Boolean
}
