package com.android.core.document.impl.office

import android.util.Log
import com.android.core.document.api.DocumentFormat
import com.android.core.document.api.document.HtmlDocument
import com.android.core.document.impl.FormatReader
import java.io.File

/**
 * [patches] run in order on a copy of the file, for what the library doesn't show (charts, first-line
 * indents). A patch that fails is skipped: the next one gets the file as it was.
 */
internal class OfficeReader(
    private val converter: OdrHtmlConverter,
    override val formats: Set<DocumentFormat>,
    private val patches: List<DocumentPatch> = emptyList()
) : FormatReader<HtmlDocument> {

    override suspend fun read(file: File, format: DocumentFormat): HtmlDocument {
        if (patches.isEmpty()) return converter.convert(file, format, variant = VARIANT_AS_IS)

        return converter.convert(
            file,
            format,
            variant = VARIANT_PATCHED,
            extraCss = { outputs -> patches.joinToString(separator = "") { patch -> patch.css(outputs) } }
        ) { original, workDirectory ->
            patches.foldIndexed(original) { index, current, patch ->
                val output = File(workDirectory, "patch-$index.${original.extension}")
                val isChanged = runCatching { patch.apply(current, output) }
                    .onFailure { error -> Log.w(TAG, "${patch::class.simpleName} failed for ${original.name}", error) }
                    .getOrDefault(false)
                if (isChanged) output else current
            }
        }
    }

    private companion object {
        const val TAG = "OfficeReader"
        const val VARIANT_AS_IS = "as-is"
        const val VARIANT_PATCHED = "patched"
    }
}
