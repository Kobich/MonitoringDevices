package com.engboost.monitoringdevices.core.document.api

/** Formats the document readers know about. New formats are added here together with a reader. */
enum class DocumentType {
    PDF,
    DOCX,
    TEXT,
    IMAGE,
    UNSUPPORTED
}

interface DocumentTypeResolver {
    /**
     * Detects the format by MIME type when it is known, otherwise by the file extension.
     * Does not read the content.
     */
    fun resolve(fileName: String, mimeType: String? = null): DocumentType

    fun resolve(source: DocumentSource): DocumentType
}
