package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.commonui.components.SelectionIndicator
import com.engboost.monitoringdevices.core.commonui.icons.FileTypeIcon
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileEntryRow(
    entry: FileEntryUi,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    ListItem(
        headlineContent = {
            Text(text = entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = entry.details, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                ) {
                    SelectionIndicator(
                        selected = isSelected,
                        modifier = Modifier.padding(end = 14.dp)
                    )
                }
                FileTypeIcon(
                    kind = entry.kind,
                    contentDescription = if (entry.isDirectory) "Folder" else "File"
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    )
}
