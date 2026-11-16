package com.android.core.fileaccess

import android.content.Context
import java.io.File
import java.io.IOException

/** The folder of the app's files, `filesDir/storage`, with a folder per feature inside; config paths start here. */
object AppFilesRoot {
    private const val DIRECTORY = "storage"

    /**
     * Not `filesDir` itself: libraries keep files there too (e.g. androidx.profileinstaller's `profileInstalled`).
     * Not `Context.dataDir`: it also holds databases/, shared_prefs/ and cache/.
     */
    fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    /**
     * [file] as a config path (`reports/2026/q1.pdf`), `null` if it isn't under [root]. Touches the file system:
     * paths are compared canonical, since `filesDir` itself may be reached through a link.
     */
    fun pathOf(file: File, root: File): String? {
        return try {
            val base = root.canonicalFile
            val target = file.canonicalFile
            if (target == base || !target.startsWith(base)) null
            else target.toRelativeString(base).replace(File.separatorChar, '/')
        } catch (error: IOException) {
            null
        }
    }
}
