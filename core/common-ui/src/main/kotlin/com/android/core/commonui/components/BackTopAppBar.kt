package com.android.core.commonui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.android.core.commonui.R
import com.android.core.commonui.icons.AppIcons
import com.android.core.commonui.theme.MonitoringDevicesTheme

/** Doesn't expose experimental Material 3 types, so callers need no opt-in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopAppBar(
    title: String,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector = AppIcons.Back,
    navigationContentDescription: String = stringResource(R.string.common_ui_back),
    /** [Color.Unspecified] keeps the theme default. */
    containerColor: Color = Color.Unspecified,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(imageVector = navigationIcon, contentDescription = navigationContentDescription)
            }
        },
        actions = actions,
        colors = if (containerColor == Color.Unspecified) {
            TopAppBarDefaults.topAppBarColors()
        } else {
            TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun BackTopAppBarPreview() {
    MonitoringDevicesTheme(dynamicColor = false) {
        BackTopAppBar(title = "Quarterly report.docx", onNavigationClick = {}) {
            IconButton(onClick = {}) {
                Icon(imageVector = AppIcons.More, contentDescription = null)
            }
        }
    }
}
