package com.android.appmanager.feature.filemanager

import android.content.Context
import java.io.File

/**
 * A folder of the app-private storage, `filesDir/storage/<name>`, that the file manager shows as its root and
 * can't go above. Each feature that keeps files has its own; the file manager's own one is [Default].
 */
class FileStorage private constructor(val name: String) {

    /** Where the files are. The folder is created when the file manager first opens it. */
    fun directory(context: Context): File = File(File(context.filesDir, ROOT_DIRECTORY), name)

    override fun equals(other: Any?): Boolean = other is FileStorage && other.name == name

    override fun hashCode(): Int = name.hashCode()

    override fun toString(): String = "FileStorage($name)"

    companion object {
        /**
         * Not `filesDir` itself: libraries keep files there too (e.g. androidx.profileinstaller's
         * `profileInstalled`). Not `Context.dataDir`: it also holds databases/, shared_prefs/ and cache/.
         */
        private const val ROOT_DIRECTORY = "storage"
        private val NAME = Regex("[a-z0-9][a-z0-9_-]{0,63}")

        /** The file manager feature's own storage, shown by the screen called without a storage. */
        val Default: FileStorage = FileStorage("file-manager")

        /**
         * Storages with the same name are the same folder.
         *
         * @throws IllegalArgumentException if [name] isn't 1–64 lowercase latin letters, digits, `-` and `_`
         * starting with a letter or a digit.
         */
        fun named(name: String): FileStorage {
            require(NAME.matches(name)) { "Invalid storage name: \"$name\"" }
            return FileStorage(name)
        }
    }
}
