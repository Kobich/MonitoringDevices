package com.engboost.monitoringdevices.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.android.core.commonui.components.BackTopAppBar
import com.android.core.fileaccess.FileAccessConfigJson

/** TEMPORARY: edits the mock config. "In 10 s" leaves time to open the file manager or a file and watch it change. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FileAccessConfigScreen(
    source: MockFileAccessConfigSource,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active by source.json.collectAsState()
    var text by rememberSaveable { mutableStateOf(active ?: MockFileAccessConfigSource.SAMPLE_JSON) }
    var scheduled by remember { mutableStateOf<String?>(null) }
    val problems = remember(text) {
        buildList { if (FileAccessConfigJson.parse(text) { problem -> add(problem) } == null && isEmpty()) add("Not a config") }
    }

    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = "File access config", onNavigationClick = onBackClick) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when {
                    active == null -> "Active: no config, everything is available"
                    active == text -> "Active: this text"
                    else -> "Active: another text, this one isn't applied"
                } + (scheduled?.let { "\nScheduled in 10 s: $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                isError = problems.isNotEmpty(),
                supportingText = {
                    Text(text = problems.joinToString(separator = "\n").ifEmpty { "Valid" })
                },
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scheduled = null; source.apply(text) }) { Text(text = "Apply") }
                OutlinedButton(onClick = { scheduled = "this text"; source.apply(text, DELAY_MILLIS) }) {
                    Text(text = "Apply in 10 s")
                }
                OutlinedButton(onClick = { scheduled = null; source.apply(null) }) { Text(text = "No config") }
                OutlinedButton(onClick = { scheduled = "no config"; source.apply(null, DELAY_MILLIS) }) {
                    Text(text = "No config in 10 s")
                }
                OutlinedButton(onClick = { text = MockFileAccessConfigSource.SAMPLE_JSON }) { Text(text = "Sample") }
            }
        }
    }
}

private const val DELAY_MILLIS = 10_000L
