package com.engboost.monitoringdevices.core.document.api

/**
 * Document category. It decides which reader/renderer handles a file and how the viewer
 * presents it (e.g. sheet tabs for spreadsheets).
 */
enum class DocumentType {
    /** PDF, rendered page by page with [com.engboost.monitoringdevices.core.document.api.pdf.PdfReader]. */
    PDF,

    /** docx, doc, odt, rtf, ... */
    TEXT_DOCUMENT,

    /** xlsx, xls, ods, csv, ... */
    SPREADSHEET,

    /** pptx, ppt, odp, ... */
    PRESENTATION,

    /** Plain text: txt, md, json, xml, log, ... */
    PLAIN_TEXT,

    IMAGE,
    UNSUPPORTED
}

interface DocumentTypeResolver {
    /**
     * Detects the category by MIME type when it is known, otherwise by the file extension.
     * Does not read the content.
     */
    fun resolve(fileName: String, mimeType: String? = null): DocumentType

    fun resolve(source: DocumentSource): DocumentType
}
