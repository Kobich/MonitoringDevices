package com.android.appmanager.feature.documentviewer.presentation.screen

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.android.core.document.api.document.HtmlDocument
import com.android.core.document.api.document.HtmlPage
import java.io.ByteArrayInputStream
import java.io.File

@Composable
internal fun HtmlDocumentContent(
    document: HtmlDocument,
    selectedPageIndex: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageIndex = selectedPageIndex.coerceIn(0, document.pages.lastIndex)

    Column(modifier = modifier) {
        if (document.pages.size > 1) {
            ScrollableTabRow(selectedTabIndex = pageIndex) {
                document.pages.forEachIndexed { index, page ->
                    Tab(
                        selected = index == pageIndex,
                        onClick = { onPageSelected(index) },
                        text = { Text(text = page.title) }
                    )
                }
            }
        }

        // A new WebView per document folder, so pages of the previous document aren't reused.
        key(document.directory) {
            HtmlPageWebView(
                directory = document.directory,
                page = document.pages[pageIndex],
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Serves the document folder through [WebViewAssetLoader]: no file or network access,
 * everything else is blocked, the content is untrusted.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HtmlPageWebView(
    directory: File,
    page: HtmlPage,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val assetLoader = remember(directory) {
        WebViewAssetLoader.Builder()
            .addPathHandler(PATH_PREFIX, WebViewAssetLoader.InternalStoragePathHandler(context, directory))
            .build()
    }
    val url = remember(directory, page) { pageUrl(directory, page.file) }

    AndroidView(
        factory = { viewContext ->
            WebView(viewContext).apply {
                settings.apply {
                    javaScriptEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webViewClient = LocalContentWebViewClient(assetLoader)
            }
        },
        update = { webView ->
            if (webView.tag != url) {
                webView.tag = url
                webView.loadUrl(url)
            }
        },
        onRelease = { webView -> webView.destroy() },
        modifier = modifier
    )
}

private class LocalContentWebViewClient(
    private val assetLoader: WebViewAssetLoader
) : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
        return assetLoader.shouldInterceptRequest(request.url) ?: blockedResponse()
    }

    /** Links inside documents don't navigate the viewer away. */
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true

    private fun blockedResponse() = WebResourceResponse(
        "text/plain",
        "utf-8",
        403,
        "Blocked",
        emptyMap(),
        ByteArrayInputStream(ByteArray(0))
    )
}

private fun pageUrl(directory: File, file: File): String {
    val relativePath = file.relativeTo(directory).invariantSeparatorsPath
        .split('/')
        .joinToString("/") { segment -> Uri.encode(segment) }
    return "https://${WebViewAssetLoader.DEFAULT_DOMAIN}$PATH_PREFIX$relativePath"
}

private const val PATH_PREFIX = "/document/"
