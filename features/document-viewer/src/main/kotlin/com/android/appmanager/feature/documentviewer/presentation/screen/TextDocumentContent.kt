package com.android.appmanager.feature.documentviewer.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.appmanager.feature.documentviewer.R
import com.android.core.document.api.document.TextDocument

/** Line by line in a lazy list, so megabytes of text don't go into one `Text`. */
@Composable
internal fun TextDocumentContent(
    document: TextDocument,
    modifier: Modifier = Modifier
) {
    val lines = remember(document) { document.text.lines() }

    Column(modifier = modifier) {
        val info = listOfNotNull(
            document.charsetName,
            pluralStringResource(R.plurals.viewer_lines, lines.size, lines.size),
            if (document.isTruncated) stringResource(R.string.viewer_text_truncated) else null
        ).joinToString(" · ")
        Text(
            text = info,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider()

        SelectionContainer {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                itemsIndexed(lines) { _, line ->
                    Text(
                        // An empty line still takes one line of height.
                        text = line.ifEmpty { " " },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
