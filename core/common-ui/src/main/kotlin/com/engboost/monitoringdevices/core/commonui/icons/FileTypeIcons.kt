package com.engboost.monitoringdevices.core.commonui.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Colors are part of the icons: draw them with [FileTypeIcon] / `Image`, not with a tinted `Icon`. */
object FileTypeIcons {
    private val cache = mutableMapOf<FileKind, ImageVector>()

    fun forKind(kind: FileKind): ImageVector = cache.getOrPut(kind) { build(kind) }

    private class Style(
        val color: Long,
        val strokes: List<String> = emptyList(),
        val fills: List<String> = emptyList()
    )

    private fun style(kind: FileKind): Style = when (kind) {
        FileKind.PDF -> Style(
            color = 0xFFE5483B,
            strokes = listOf(
                "M8.6,18.3C9.6,16.4 11.3,12.6 11.5,11.2C11.7,9.9 10.6,9.9 10.6,11C10.6,12.9 13.4,16.1 15.6,16.2" +
                    "C16.6,16.25 16.6,15.3 15.6,15.25C13.6,15.15 10.4,16.6 9.4,17.6C8.8,18.2 8.3,18.8 8.6,18.3Z"
            )
        )

        FileKind.DOCUMENT -> Style(0xFF2F6FE4, strokes = listOf("M8,11.5H13", "M8,14.25H16", "M8,17H16"))
        FileKind.SPREADSHEET -> Style(
            color = 0xFF1E8E3E,
            strokes = listOf("M8,10.75H16V18.25H8Z", "M11.25,10.75V18.25", "M8,13.25H16", "M8,15.75H16")
        )

        FileKind.PRESENTATION -> Style(
            color = 0xFFE8710A,
            strokes = listOf("M7.75,11H16.25V16.5H7.75Z", "M12,16.5V18.75", "M10,18.75H14")
        )

        FileKind.TEXT -> Style(0xFF6B7280, strokes = listOf("M8,11.5H16", "M8,14.25H16", "M8,17H12.5"))
        FileKind.IMAGE -> Style(
            color = 0xFF8E44D9,
            fills = listOf(
                "M7.5,18.5L10.6,14.2L12.7,16.8L14.2,15.1L16.5,18.5Z",
                "M14.9,11.6m-1.35,0a1.35,1.35 0 1,0 2.7,0a1.35,1.35 0 1,0 -2.7,0"
            )
        )

        FileKind.AUDIO -> Style(
            color = 0xFFD63384,
            strokes = listOf("M12.25,16.75V10.5L15.75,11.5"),
            fills = listOf("M10.6,16.75m-1.65,0a1.65,1.65 0 1,0 3.3,0a1.65,1.65 0 1,0 -3.3,0")
        )

        FileKind.VIDEO -> Style(
            color = 0xFF0E9AA7,
            fills = listOf("M10,11.2V17.8A0.6,0.6 0 0 0 10.9,18.3L15.9,15.0A0.6,0.6 0 0 0 15.9,14.0L10.9,10.7A0.6,0.6 0 0 0 10,11.2Z")
        )

        FileKind.ARCHIVE -> Style(
            color = 0xFF8D6E63,
            fills = listOf(
                "M11,9H12.5V10.5H11Z",
                "M12.5,10.5H14V12H12.5Z",
                "M11,12H12.5V13.5H11Z",
                "M12.5,13.5H14V15H12.5Z",
                "M11,15.5H14V18.5H11Z"
            )
        )

        FileKind.OTHER, FileKind.FOLDER -> Style(0xFF9AA0A6)
    }

    private fun build(kind: FileKind): ImageVector {
        val builder = ImageVector.Builder(
            name = "FileType.$kind",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )

        if (kind == FileKind.FOLDER) {
            builder.fill(FOLDER_BACK, Color(0xFFE8A200))
            builder.fill(FOLDER_FRONT, Color(0xFFFBBC04))
            return builder.build()
        }

        val style = style(kind)
        builder.fill(PAGE, Color(style.color))
        builder.fill(PAGE_FOLD, Color.White, alpha = 0.45f)
        style.fills.forEach { path -> builder.fill(path, Color.White) }
        style.strokes.forEach { path ->
            builder.addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    private fun ImageVector.Builder.fill(path: String, color: Color, alpha: Float = 1f) {
        addPath(pathData = addPathNodes(path), fill = SolidColor(color), fillAlpha = alpha)
    }

    private const val PAGE = "M6,2H14L20,8V20A2,2 0 0 1 18,22H6A2,2 0 0 1 4,20V4A2,2 0 0 1 6,2Z"
    private const val PAGE_FOLD = "M14,2V6.5A1.5,1.5 0 0 0 15.5,8H20Z"
    private const val FOLDER_BACK =
        "M4,4H9.2L11.2,6H20A2,2 0 0 1 22,8V18A2,2 0 0 1 20,20H4A2,2 0 0 1 2,18V6A2,2 0 0 1 4,4Z"
    private const val FOLDER_FRONT = "M2,9.5A2,2 0 0 1 4,7.5H20A2,2 0 0 1 22,9.5V18A2,2 0 0 1 20,20H4A2,2 0 0 1 2,18Z"
}

@Composable
fun FileTypeIcon(
    kind: FileKind,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    contentDescription: String? = null
) {
    Image(
        imageVector = FileTypeIcons.forKind(kind),
        contentDescription = contentDescription,
        modifier = modifier.size(size)
    )
}
