package com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.document.api.comparison.ChartRendering
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.DocumentViewerUiState
import com.engboost.monitoringdevices.feature.viewer.impl.presentation.model.ViewerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocumentViewerScreen(
    state: DocumentViewerUiState,
    onBackClick: () -> Unit,
    onChartRenderingSelected: (ChartRendering) -> Unit,
    onPageSelected: (Int) -> Unit,
    onRetryClick: () -> Unit,
    onPasswordSubmit: (String) -> Unit,
    onPasswordDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    state.passwordDialog?.let { dialog ->
        PasswordDialog(error = dialog.error, onSubmit = onPasswordSubmit, onDismiss = onPasswordDismiss)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = state.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            state.chartRendering?.let { selected ->
                ChartRenderingSwitch(
                    selected = selected,
                    isEnabled = state.content !is ViewerContent.Loading,
                    onSelected = onChartRenderingSelected
                )
                HorizontalDivider()
            }

            when (val content = state.content) {
                ViewerContent.Loading -> CenteredMessage {
                    CircularProgressIndicator()
                    Text(text = "Rendering document…", style = MaterialTheme.typography.bodyMedium)
                }

                is ViewerContent.Html -> HtmlDocumentContent(
                    document = content.document,
                    selectedPageIndex = content.selectedPageIndex,
                    onPageSelected = onPageSelected,
                    modifier = Modifier.fillMaxSize()
                )

                is ViewerContent.NotImplemented -> CenteredMessage {
                    Text(
                        text = "Viewing ${content.kind} files is not implemented yet",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }

                is ViewerContent.Error -> CenteredMessage {
                    Text(
                        text = content.message,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onRetryClick) {
                        Text(text = "Retry")
                    }
                }
            }
        }
    }
}

/** TEMPORARY: switch for the chart comparison, see `ChartRendering`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartRenderingSwitch(
    selected: ChartRendering,
    isEnabled: Boolean,
    onSelected: (ChartRendering) -> Unit
) {
    val options = listOf(ChartRendering.AS_IS to "As is", ChartRendering.AS_PICTURE to "As picture")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Charts", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            options.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    enabled = isEnabled,
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                ) {
                    Text(text = label)
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
        }
    }
}
