package com.android.appmanager.feature.documentviewer.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.android.appmanager.feature.documentviewer.R
import java.io.File

/** The authority of the provider in this module's manifest. */
private const val AUTHORITY_SUFFIX = ".documentviewer.files"

/** Hands the original file to an app the user picks, to compare it with this viewer. */
internal fun openInOtherApp(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
        val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
        val view = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, type)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(view, null))
    } catch (error: IllegalArgumentException) {
        // getUriForFile: the file is outside the folders the provider shares.
        Toast.makeText(context, R.string.viewer_open_in_other_app_failed, Toast.LENGTH_SHORT).show()
    } catch (error: ActivityNotFoundException) {
        Toast.makeText(context, R.string.viewer_open_in_other_app_failed, Toast.LENGTH_SHORT).show()
    }
}
