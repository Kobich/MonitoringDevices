package com.engboost.monitoringdevices.core.commonui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * All app icons: features use these, not `Icons.*` or drawables.
 * Colored file icons are in [FileTypeIcons].
 */
object AppIcons {
    val Back: ImageVector get() = Icons.AutoMirrored.Filled.ArrowBack
    val Close: ImageVector get() = Icons.Filled.Close
    val Delete: ImageVector get() = Icons.Filled.Delete
    val More: ImageVector get() = Icons.Filled.MoreVert
    val Add: ImageVector get() = Icons.Filled.Add
    val Check: ImageVector get() = Icons.Filled.Check
    val Play: ImageVector get() = Icons.Filled.PlayArrow

    val Pause: ImageVector by lazy {
        icon(name = "Pause", pathData = "M6,19h4V5H6v14zM14,5v14h4V5h-4z")
    }

    val Folder: ImageVector by lazy {
        icon(
            name = "Folder",
            pathData = "M10,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8" +
                "c0,-1.1 -0.9,-2 -2,-2h-8l-2,-2z"
        )
    }

    val File: ImageVector by lazy {
        icon(
            name = "File",
            pathData = "M6,2c-1.1,0 -1.99,0.9 -1.99,2L4,20c0,1.1 0.89,2 1.99,2H18c1.1,0 2,-0.9 2,-2V8" +
                "l-6,-6H6zM13,9V3.5L18.5,9H13z"
        )
    }

    private fun icon(name: String, pathData: String): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = addPathNodes(pathData),
            fill = SolidColor(Color.Black)
        ).build()
    }
}
