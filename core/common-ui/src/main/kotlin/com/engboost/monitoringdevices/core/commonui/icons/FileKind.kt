package com.engboost.monitoringdevices.core.commonui.icons

/** Visual kind of a file system entry, decides its icon. Detection uses only the name. */
enum class FileKind {
    FOLDER,
    PDF,
    DOCUMENT,
    SPREADSHEET,
    PRESENTATION,
    TEXT,
    IMAGE,
    AUDIO,
    VIDEO,
    ARCHIVE,
    OTHER;

    companion object {
        fun of(fileName: String, isDirectory: Boolean): FileKind {
            if (isDirectory) return FOLDER
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            return BY_EXTENSION[extension] ?: OTHER
        }

        private val BY_EXTENSION: Map<String, FileKind> = buildMap {
            fun register(kind: FileKind, vararg extensions: String) = extensions.forEach { put(it, kind) }

            register(PDF, "pdf")
            register(DOCUMENT, "doc", "docx", "docm", "dot", "dotx", "odt", "rtf", "pages")
            register(SPREADSHEET, "xls", "xlsx", "xlsm", "ods", "csv", "tsv", "numbers")
            register(PRESENTATION, "ppt", "pptx", "pps", "ppsx", "odp", "key")
            register(TEXT, "txt", "md", "log", "json", "xml", "yaml", "yml", "ini", "html", "htm")
            register(IMAGE, "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tif", "tiff")
            register(AUDIO, "mp3", "wav", "ogg", "oga", "flac", "m4a", "aac", "opus")
            register(VIDEO, "mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v")
            register(ARCHIVE, "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz")
        }
    }
}
