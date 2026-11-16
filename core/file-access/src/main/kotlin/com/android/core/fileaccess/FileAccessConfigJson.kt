package com.android.core.fileaccess

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads the config as the administrator writes it:
 *
 * ```
 * { "parameters": {
 *     "menu": [ { "key": "import" | "export" | "delete", "password": "" } ],
 *     "initial_files": [ { "path": "file-manager/Contract.pdf", "message": "", "password": "" } ]
 * } }
 * ```
 *
 * An empty or missing `password` or `message` is not set. Without `menu` every action is available, with it only the
 * listed ones. Other fields (`code_sequence`) are ignored.
 */
object FileAccessConfigJson {

    /**
     * `null` if [json] isn't a config at all; that means no config, everything is available. A bad entry (unknown
     * key, invalid path, a repeated key or path) is skipped and reported to [onProblem].
     */
    fun parse(json: String, onProblem: (String) -> Unit = {}): FileAccessConfig? {
        return try {
            val root = JSONObject(json)
            val parameters = root.optJSONObject("parameters") ?: root
            FileAccessConfig(
                actions = if (parameters.has("menu")) menu(parameters.getJSONArray("menu"), onProblem) else null,
                files = if (parameters.has("initial_files")) files(parameters.getJSONArray("initial_files"), onProblem) else emptyMap()
            )
        } catch (error: JSONException) {
            onProblem("Not a config: ${error.message}")
            null
        }
    }

    private fun menu(items: JSONArray, onProblem: (String) -> Unit): Map<FileAction, Password?> {
        val actions = linkedMapOf<FileAction, Password?>()
        items.objects(onProblem).forEach { item ->
            val key = item.text("key")
            val action = key?.let(FileAction::fromKey)
            when {
                action == null -> onProblem("Unknown menu key: $key")
                // "select" was dropped from the format: an unavailable action is left out of the menu.
                item.optBoolean("select", true).not() -> Unit
                action in actions -> onProblem("Repeated menu key: $key")
                else -> actions[action] = item.text("password")?.let(::Password)
            }
        }
        return actions
    }

    private fun files(items: JSONArray, onProblem: (String) -> Unit): Map<String, FileRule> {
        val files = linkedMapOf<String, FileRule>()
        items.objects(onProblem).forEach { item ->
            val path = item.text("path")?.let { path -> runCatching { FileAccessConfig.normalizePath(path) }.getOrNull() }
            when {
                path == null -> onProblem("Invalid file path: ${item.opt("path")}")
                path in files -> onProblem("Repeated file path: $path")
                else -> files[path] = FileRule(message = item.text("message"), password = item.text("password")?.let(::Password))
            }
        }
        return files
    }

    private fun JSONArray.objects(onProblem: (String) -> Unit): List<JSONObject> = (0 until length()).mapNotNull { index ->
        val item = optJSONObject(index)
        if (item == null) onProblem("Not an object: ${opt(index)}")
        item
    }

    /** A non-empty string; `optString` would turn JSON `null` into "null". */
    private fun JSONObject.text(name: String): String? =
        if (isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }
}
