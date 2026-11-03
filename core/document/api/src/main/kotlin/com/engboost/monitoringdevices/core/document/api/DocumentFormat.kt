package com.engboost.monitoringdevices.core.document.api

/** What kind of content a format holds; decides how the viewer presents it. */
enum class DocumentCategory {
    PDF,
    TEXT_DOCUMENT,
    SPREADSHEET,
    PRESENTATION,
    PLAIN_TEXT,
    IMAGE
}

/** Every format the document readers know about. Each one has exactly one reader. */
enum class DocumentFormat(
    val category: DocumentCategory,
    val extensions: Set<String>,
    val mimeTypes: Set<String>
) {
    PDF(DocumentCategory.PDF, setOf("pdf"), setOf("application/pdf")),

    DOCX(
        DocumentCategory.TEXT_DOCUMENT,
        setOf("docx"),
        setOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    ),
    DOC(DocumentCategory.TEXT_DOCUMENT, setOf("doc"), setOf("application/msword")),
    ODT(DocumentCategory.TEXT_DOCUMENT, setOf("odt"), setOf("application/vnd.oasis.opendocument.text")),
    RTF(DocumentCategory.TEXT_DOCUMENT, setOf("rtf"), setOf("application/rtf", "text/rtf")),

    XLSX(
        DocumentCategory.SPREADSHEET,
        setOf("xlsx"),
        setOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ),
    XLS(DocumentCategory.SPREADSHEET, setOf("xls"), setOf("application/vnd.ms-excel")),
    ODS(DocumentCategory.SPREADSHEET, setOf("ods"), setOf("application/vnd.oasis.opendocument.spreadsheet")),
    CSV(DocumentCategory.SPREADSHEET, setOf("csv"), setOf("text/csv", "text/comma-separated-values")),

    PPTX(
        DocumentCategory.PRESENTATION,
        setOf("pptx"),
        setOf("application/vnd.openxmlformats-officedocument.presentationml.presentation")
    ),
    PPT(DocumentCategory.PRESENTATION, setOf("ppt"), setOf("application/vnd.ms-powerpoint")),
    ODP(DocumentCategory.PRESENTATION, setOf("odp"), setOf("application/vnd.oasis.opendocument.presentation")),

    TEXT(
        DocumentCategory.PLAIN_TEXT,
        setOf("txt", "log", "md", "json", "xml"),
        setOf("text/plain", "text/markdown", "application/json", "text/xml", "application/xml")
    ),

    IMAGE(
        DocumentCategory.IMAGE,
        setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif"),
        setOf("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp", "image/heic", "image/heif")
    );

    companion object {
        /** By MIME type when it is specific, otherwise by extension. `null` if unsupported. */
        fun of(fileName: String, mimeType: String? = null): DocumentFormat? {
            return mimeType?.let(::fromMimeType) ?: fromFileName(fileName)
        }

        fun of(source: DocumentSource): DocumentFormat? {
            val mimeType = (source as? DocumentSource.Content)?.mimeType
            return of(source.name, mimeType)
        }

        fun fromFileName(fileName: String): DocumentFormat? {
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            if (extension.isEmpty()) return null
            return entries.firstOrNull { format -> extension in format.extensions }
        }

        fun fromMimeType(mimeType: String): DocumentFormat? {
            val normalized = mimeType.substringBefore(';').trim().lowercase()
            return entries.firstOrNull { format -> normalized in format.mimeTypes }
        }
    }
}
