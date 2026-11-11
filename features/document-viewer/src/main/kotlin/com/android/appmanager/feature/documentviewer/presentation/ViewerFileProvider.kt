package com.android.appmanager.feature.documentviewer.presentation

import androidx.core.content.FileProvider

/** Its own class, so the manifest merger doesn't clash with another module's `FileProvider`. */
internal class ViewerFileProvider : FileProvider()
