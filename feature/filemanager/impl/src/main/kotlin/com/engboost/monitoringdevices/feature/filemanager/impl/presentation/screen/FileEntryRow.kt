package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import com.engboost.monitoringdevices.core.commonui.icons.AppIcons
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi

/** Stateless row: the ViewModel decides what tap and long press mean in the current mode. */
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
            Icon(
                imageVector = if (entry.isDirectory) AppIcons.Folder else AppIcons.File,
                contentDescription = if (entry.isDirectory) "Folder" else "File",
                tint = if (entry.isDirectory) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        trailingContent = if (isSelectionMode) {
            { Checkbox(checked = isSelected, onCheckedChange = null) }
        } else {
            null
        },
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
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
