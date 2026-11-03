package com.engboost.monitoringdevices.core.document.api.richtext

/**
 * Format-independent model of a flowing text document (DOCX now, maybe ODT/RTF later).
 * It keeps only what a viewer needs: structure and basic styling, not exact layout.
 */
data class RichTextDocument(
    val title: String?,
    val blocks: List<RichTextBlock>
)

sealed interface RichTextBlock {
    data class Paragraph(
        val spans: List<TextSpan>,
        val style: ParagraphStyle = ParagraphStyle()
    ) : RichTextBlock

    data class Table(
        val rows: List<TableRow>
    ) : RichTextBlock

    /** Embedded picture. Bytes are loaded on demand to keep large documents light. */
    class Image(
        val mimeType: String,
        val widthPx: Int?,
        val heightPx: Int?,
        val description: String?,
        val loadBytes: suspend () -> ByteArray
    ) : RichTextBlock

    data object PageBreak : RichTextBlock
}

data class TextSpan(
    val text: String,
    val style: SpanStyle = SpanStyle()
)

data class SpanStyle(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    /** Target of a hyperlink, `null` for plain text. */
    val link: String? = null
)

data class ParagraphStyle(
    /** 1..6 for headings, `null` for body text. */
    val headingLevel: Int? = null,
    val list: ListMarker? = null,
    val alignment: Alignment = Alignment.START
) {
    enum class Alignment { START, CENTER, END, JUSTIFY }
}

data class ListMarker(
    /** Nesting level starting from 0. */
    val level: Int,
    val isOrdered: Boolean,
    /** Position inside an ordered list starting from 1, `null` for bullets. */
    val number: Int?
)

data class TableRow(
    val cells: List<TableCell>
)

data class TableCell(
    val blocks: List<RichTextBlock>,
    val columnSpan: Int = 1
)
