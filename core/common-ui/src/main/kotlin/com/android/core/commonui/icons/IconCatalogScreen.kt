package com.android.core.commonui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.core.commonui.components.BackTopAppBar
import com.android.core.commonui.theme.MonitoringDevicesTheme

/** Every icon of the module on one screen. A new [AppIcons] entry goes into [appIcons] too. */
@Composable
fun IconCatalogScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = { BackTopAppBar(title = "Icons", onNavigationClick = onBackClick) }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 96.dp),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            header("AppIcons")
            items(appIcons, key = { (name, _) -> "app-$name" }) { (name, icon) ->
                IconTile(name = name) { Icon(imageVector = icon, contentDescription = null) }
            }

            header("FileTypeIcons")
            items(FileKind.entries, key = { kind -> "file-${kind.name}" }) { kind ->
                IconTile(name = kind.name) { FileTypeIcon(kind = kind) }
            }
        }
    }
}

private val appIcons: List<Pair<String, ImageVector>>
    get() = listOf(
        "Back" to AppIcons.Back,
        "Close" to AppIcons.Close,
        "Delete" to AppIcons.Delete,
        "More" to AppIcons.More,
        "Add" to AppIcons.Add,
        "Check" to AppIcons.Check,
        "Play" to AppIcons.Play,
        "Pause" to AppIcons.Pause,
        "Folder" to AppIcons.Folder,
        "File" to AppIcons.File,
        "Link" to AppIcons.Link,
        "LinkOff" to AppIcons.LinkOff
    )

private fun LazyGridScope.header(title: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun IconTile(name: String, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IconCatalogScreenPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        IconCatalogScreen(onBackClick = {})
    }
}
