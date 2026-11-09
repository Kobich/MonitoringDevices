package com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.engboost.monitoringdevices.core.document.api.document.PageSize
import com.engboost.monitoringdevices.core.document.api.document.PdfDocument
import com.engboost.monitoringdevices.feature.viewer.impl.R

@Composable
internal fun PdfDocumentContent(
    document: PdfDocument,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentPage by remember { derivedStateOf { listState.firstVisibleItemIndex + 1 } }

    BoxWithConstraints(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        val horizontalPadding = 8.dp
        val pageWidthPx = with(LocalDensity.current) { (maxWidth - horizontalPadding * 2).roundToPx() }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(count = document.pageCount, key = { index -> index }) { index ->
                PdfPage(document = document, pageIndex = index, widthPx = pageWidthPx)
            }
        }

        if (document.pageCount > 1) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.viewer_page_counter, currentPage, document.pageCount),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun PdfPage(
    document: PdfDocument,
    pageIndex: Int,
    widthPx: Int
) {
    val pageSize by produceState<PageSize?>(initialValue = null, document, pageIndex) {
        value = runCatching { document.pageSize(pageIndex) }.getOrNull()
    }
    val rendered by produceState<Result<Bitmap>?>(initialValue = null, document, pageIndex, widthPx) {
        value = runCatching { document.renderPage(pageIndex, widthPx) }
    }
    val aspectRatio = pageSize?.let { size -> size.widthPt.toFloat() / size.heightPt } ?: A4_ASPECT_RATIO

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val result = rendered
        val page = result?.getOrNull()
        when {
            page != null -> Image(
                bitmap = page.asImageBitmap(),
                contentDescription = stringResource(R.string.viewer_page, pageIndex + 1),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )

            result == null -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            else -> Text(
                text = stringResource(R.string.viewer_page_failed, pageIndex + 1),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
        }
    }
}

private const val A4_ASPECT_RATIO = 210f / 297f
