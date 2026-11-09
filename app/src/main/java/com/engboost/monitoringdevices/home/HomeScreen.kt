package com.engboost.monitoringdevices.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeScreen(
    onFileManagerClick: () -> Unit,
    onScannerClick: () -> Unit,
    onFeatureTestClick: () -> Unit,
    onUiCatalogClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onFileManagerClick) {
            Text(text = "File manager")
        }
        Button(onClick = onScannerClick) {
            Text(text = "Open nearby scanner")
        }
        Button(onClick = onFeatureTestClick) {
            Text(text = "Test features")
        }
        Button(onClick = onUiCatalogClick) {
            Text(text = "UI catalog")
        }
    }
}
