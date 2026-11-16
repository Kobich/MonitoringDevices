package com.android.core.fileaccess

import java.security.MessageDigest

/**
 * The administrator's rules for the app's files, one config for the whole app: which file manager actions are
 * available and with which password, which files open only with a password or show a message instead of opening.
 * [Unrestricted] stands for no config: everything is available without passwords.
 *
 * @param actions available actions with their passwords (`null`: no password); an action that isn't here is
 * unavailable. `null` map: every action is available without a password.
 * @param files rules by file path from [AppFilesRoot], see [normalizePath].
 * @throws IllegalArgumentException if a file path is invalid or two paths are the same file.
 */
class FileAccessConfig(
    val actions: Map<FileAction, Password?>?,
    files: Map<String, FileRule>
) {
    val files: Map<String, FileRule> = files.entries.associate { (path, rule) -> normalizePath(path) to rule }

    init {
        require(this.files.size == files.size) { "Paths of the same file: ${files.keys}" }
    }

    fun access(action: FileAction): Access {
        if (actions == null) return Access.Allowed
        if (action !in actions) return Access.Hidden
        return actions[action]?.let(Access::PasswordRequired) ?: Access.Allowed
    }

    /** For a file at [path] from [AppFilesRoot]; never [Access.Hidden]. A message comes before a password. */
    fun access(path: String): Access {
        val rule = runCatching { files[normalizePath(path)] }.getOrNull() ?: return Access.Allowed
        rule.message?.let { message -> return Access.Message(message) }
        rule.password?.let { password -> return Access.PasswordRequired(password) }
        return Access.Allowed
    }

    override fun equals(other: Any?): Boolean =
        other is FileAccessConfig && other.actions == actions && other.files == files

    override fun hashCode(): Int = 31 * actions.hashCode() + files.hashCode()

    override fun toString(): String = "FileAccessConfig(actions=${actions?.keys}, files=${files.size})"

    companion object {
        val Unrestricted = FileAccessConfig(actions = null, files = emptyMap())

        /**
         * Segments separated by `/` (or `\`), from [AppFilesRoot]: `file-manager/Contract.pdf`. Leading, trailing and
         * repeated separators and `.` segments are dropped; case matters.
         *
         * @throws IllegalArgumentException for an empty path or one with `..`.
         */
        fun normalizePath(path: String): String {
            val segments = path.split('/', '\\').filter { segment -> segment.isNotEmpty() && segment != "." }
            require(segments.isNotEmpty()) { "Empty path: \"$path\"" }
            require(segments.none { segment -> segment == ".." }) { "Path with \"..\": \"$path\"" }
            return segments.joinToString(separator = "/")
        }
    }
}

/** File manager actions the config can turn off or protect with a password, by their key in the config. */
enum class FileAction(val key: String) {
    /** Importing files and folders. */
    IMPORT("import"),

    /** Exporting, including the viewer's "Open in another app". */
    EXPORT("export"),
    DELETE("delete");

    companion object {
        fun fromKey(key: String): FileAction? = entries.firstOrNull { action -> action.key == key }
    }
}

/** [message] (shown instead of opening the file) and [password] are `null` when not set. */
class FileRule(message: String?, val password: Password?) {
    val message: String? = message?.takeIf { it.isNotBlank() }

    override fun equals(other: Any?): Boolean = other is FileRule && other.message == message && other.password == password

    override fun hashCode(): Int = 31 * message.hashCode() + password.hashCode()
}

sealed interface Access {
    data object Allowed : Access

    /** The action is unavailable: hide it. */
    data object Hidden : Access

    /** Asked each time the action runs or the file opens. */
    data class PasswordRequired(val password: Password) : Access

    /** Shown instead of opening the file. */
    data class Message(val text: String) : Access
}

/** Kept as the config gives it for now; a hash can take its place without changes for callers. */
class Password(private val text: String) {
    init {
        require(text.isNotEmpty()) { "Empty password" }
    }

    fun matches(input: String): Boolean = MessageDigest.isEqual(text.toByteArray(), input.toByteArray())

    override fun equals(other: Any?): Boolean = other is Password && other.text == text

    override fun hashCode(): Int = text.hashCode()

    /** Never shows the password: it could end up in logs. */
    override fun toString(): String = "Password(***)"
}
