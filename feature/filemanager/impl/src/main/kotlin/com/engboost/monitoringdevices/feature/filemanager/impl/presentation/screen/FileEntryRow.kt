package com.engboost.monitoringdevices.feature.filemanager.impl.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.feature.filemanager.impl.R
import com.engboost.monitoringdevices.feature.filemanager.impl.presentation.model.FileEntryUi

/**
 * Tap opens a folder; for a file tap shows the actions menu.
 * Long press or the trailing button shows the actions menu for any entry.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FileEntryRow(
    entry: FileEntryUi,
    isActionsEnabled: Boolean,
    onOpenClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    Box(modifier = modifier) {
        ListItem(
            headlineContent = {
                Text(text = entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(text = entry.details, maxLines = 1, overflow = TextOverflow.Ellipsis)
            },
            leadingContent = {
                Icon(
                    painter = painterResource(if (entry.isDirectory) R.drawable.ic_folder else R.drawable.ic_file),
                    contentDescription = if (entry.isDirectory) "Folder" else "File",
                    tint = if (entry.isDirectory) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            trailingContent = {
                IconButton(onClick = { isMenuExpanded = true }, enabled = isActionsEnabled) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Actions for ${entry.name}")
                }
            },
            modifier = Modifier.combinedClickable(
                onClick = {
                    if (entry.isDirectory) {
                        onOpenClick()
                    } else if (isActionsEnabled) {
                        isMenuExpanded = true
                    }
                },
                onLongClick = {
                    if (isActionsEnabled) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        isMenuExpanded = true
                    }
                }
            )
        )

        DropdownMenu(
            expanded = isMenuExpanded && isActionsEnabled,
            onDismissRequest = { isMenuExpanded = false },
            offset = DpOffset(x = 56.dp, y = 0.dp)
        ) {
            if (entry.isDirectory) {
                DropdownMenuItem(
                    text = { Text(text = "Open") },
                    onClick = {
                        isMenuExpanded = false
                        onOpenClick()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(text = "Export") },
                onClick = {
                    isMenuExpanded = false
                    onExportClick()
                }
            )
            DropdownMenuItem(
                text = { Text(text = "Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    isMenuExpanded = false
                    onDeleteClick()
                }
            )
        }
    }
}
