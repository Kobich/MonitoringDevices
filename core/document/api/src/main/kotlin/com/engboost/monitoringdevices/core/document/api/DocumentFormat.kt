package com.engboost.monitoringdevices.core.document.api

import java.io.File

/** Supported formats, recognized by file extension. */
enum class DocumentFormat(vararg extensions: String) {
    PDF("pdf"),

    DOCX("docx"),
    DOC("doc"),
    ODT("odt", "fodt"),
    RTF("rtf"),
    PAGES("pages"),
    MARKDOWN("md", "markdown"),

    XLSX("xlsx"),
    XLS("xls"),
    ODS("ods", "fods"),
    CSV("csv"),
    NUMBERS("numbers"),

    PPTX("pptx"),
    PPT("ppt"),
    ODP("odp", "fodp"),
    KEYNOTE("key"),

    ODG("odg", "fodg"),

    TEXT("txt", "log", "json", "xml"),
    IMAGE("png", "jpg", "jpeg", "gif", "webp", "bmp", "heic", "heif"),
    SVG("svg"),
    AUDIO("mp3", "wav", "m4a", "aac", "ogg", "oga", "opus", "flac");

    val extensions: Set<String> = extensions.toSet()

    companion object {
        /** `null` if the file type isn't supported. */
        fun of(file: File): DocumentFormat? {
            val extension = file.extension.lowercase()
            return entries.firstOrNull { format -> extension in format.extensions }
        }
    }
}
