package com.android.core.commonui.icons

/** What a file system entry is, by its name only; decides its [FileTypeIcon]. */
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
        /** Kind by the extension of [fileName], ignoring case; [OTHER] for an unknown one. */
        fun of(fileName: String, isDirectory: Boolean): FileKind {
            if (isDirectory) return FOLDER
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            return BY_EXTENSION[extension] ?: OTHER
        }

        private val BY_EXTENSION: Map<String, FileKind> = buildMap {
            fun register(kind: FileKind, vararg extensions: String) = extensions.forEach { put(it, kind) }

            register(PDF, "pdf")
            register(DOCUMENT, "doc", "docx", "docm", "dot", "dotx", "odt", "fodt", "rtf", "pages")
            register(SPREADSHEET, "xls", "xlsx", "xlsm", "ods", "fods", "csv", "tsv", "numbers")
            register(PRESENTATION, "ppt", "pptx", "pps", "ppsx", "odp", "fodp", "key")
            register(TEXT, "txt", "md", "markdown", "log", "json", "xml", "yaml", "yml", "ini", "html", "htm")
            register(IMAGE, "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg", "tif", "tiff", "odg", "fodg")
            register(AUDIO, "mp3", "wav", "ogg", "oga", "flac", "m4a", "aac", "opus")
            register(VIDEO, "mp4", "mkv", "webm", "avi", "mov", "3gp", "m4v")
            register(ARCHIVE, "zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz")
        }
    }
}
