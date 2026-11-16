package com.engboost.monitoringdevices.config

import android.content.Context
import android.util.Log
import com.android.core.fileaccess.FileAccessConfig
import com.android.core.fileaccess.FileAccessConfigJson
import com.android.core.fileaccess.FileAccessConfigSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MockFileAccessConfig"

/**
 * TEMPORARY: stands in for the store module until it gives the config. Keeps the config JSON in SharedPreferences
 * (the store keeps it in DataStore) and lets the config screen change it while the features are open.
 */
@Singleton
internal class MockFileAccessConfigSource @Inject constructor(
    @ApplicationContext context: Context
) : FileAccessConfigSource {

    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var delayedJob: Job? = null

    private val _json = MutableStateFlow(
        if (preferences.contains(KEY_JSON)) preferences.getString(KEY_JSON, null) else SAMPLE_JSON
    )

    /** `null`: no config. */
    val json: StateFlow<String?> = _json.asStateFlow()

    override val config: Flow<FileAccessConfig?> = _json.map { json ->
        json?.let { FileAccessConfigJson.parse(it) { problem -> Log.w(TAG, problem) } }
    }

    fun apply(json: String?, delayMillis: Long = 0) {
        delayedJob?.cancel()
        delayedJob = scope.launch {
            delay(delayMillis)
            Log.d(TAG, "apply: ${if (json == null) "no config" else "${json.length} chars"}")
            preferences.edit().putString(KEY_JSON, json).apply()
            _json.value = json
        }
    }

    companion object {
        private const val PREFERENCES = "file_access_config"
        private const val KEY_JSON = "json"

        /** For the sample files: passwords to export and delete, a password and a message on two files. */
        val SAMPLE_JSON = """
            {
              "parameters": {
                "menu": [
                  { "key": "import", "password": "" },
                  { "key": "export", "password": "1111" },
                  { "key": "delete", "password": "2222" }
                ],
                "initial_files": [
                  { "path": "file-manager/Quarterly report.docx", "message": "", "password": "1234" },
                  { "path": "file-manager/Budget.csv", "message": "The budget is available on request from the administrator", "password": "" }
                ]
              }
            }
        """.trimIndent()
    }
}
