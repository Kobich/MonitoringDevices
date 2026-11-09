package com.android.core.document.api

import java.io.File

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

    TXT("txt"),
    LOG("log"),
    JSON("json"),
    XML("xml"),

    PNG("png"),
    JPEG("jpg", "jpeg"),
    GIF("gif"),
    WEBP("webp"),
    BMP("bmp"),
    HEIF("heic", "heif"),
    SVG("svg"),

    MP3("mp3"),
    WAV("wav"),
    M4A("m4a"),
    AAC("aac"),
    OGG("ogg", "oga"),
    OPUS("opus"),
    FLAC("flac"),

    MP4("mp4"),
    M4V("m4v"),
    MOV("mov"),
    THREE_GP("3gp"),
    WEBM("webm"),
    MKV("mkv");

    val extensions: Set<String> = extensions.toSet()

    companion object {
        fun of(file: File): DocumentFormat? {
            val extension = file.extension.lowercase()
            return entries.firstOrNull { format -> extension in format.extensions }
        }
    }
}
