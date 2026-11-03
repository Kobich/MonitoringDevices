package com.engboost.monitoringdevices.sample

import android.content.Context
import android.util.Log
import com.engboost.monitoringdevices.sample.SampleDocx.Block
import com.engboost.monitoringdevices.sample.SampleDocx.Chart
import java.io.File

/**
 * TEMPORARY: puts example files into the app-private storage on the first launch so the
 * file manager and the viewer have something to show. Remove this package and its call
 * in MonitoringDevicesApplication before release.
 */
internal class SampleFilesSeeder(
    private val context: Context
) {
    fun seedOnce() {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (preferences.getBoolean(KEY_SEEDED, false)) return

        runCatching { seed(context.filesDir) }
            .onSuccess { preferences.edit().putBoolean(KEY_SEEDED, true).apply() }
            .onFailure { error -> Log.w(TAG, "Can't create sample files", error) }
    }

    private fun seed(root: File) {
        root.writeSample("Readme.txt", README.toByteArray())
        root.writeSample("Quarterly report.docx", quarterlyReport())
        root.writeSample("Budget.csv", BUDGET_CSV.toByteArray())

        val samples = File(root, "Samples").apply { mkdirs() }
        samples.writeSample("Market share.docx", marketShare())
        samples.writeSample("Sales by region.csv", REGIONS_CSV.toByteArray())
        samples.writeSample("Notes.md", NOTES.toByteArray())

        File(samples, "Empty folder").mkdirs()

        val media = File(root, "Media").apply { mkdirs() }
        media.writeSample("Sample.pdf", SampleMedia.pdf())
        media.writeSample("Picture.png", SampleMedia.png())
        media.writeSample("Chord.wav", SampleMedia.wav())
        media.writeSample("Привет.txt", RUSSIAN_TEXT.toByteArray(Charsets.UTF_8))
        media.writeSample("Old encoding (cp1251).txt", RUSSIAN_TEXT.toByteArray(charset("windows-1251")))
    }

    private fun File.writeSample(name: String, content: ByteArray) {
        val file = File(this, name)
        if (!file.exists()) file.writeBytes(content)
    }

    private fun quarterlyReport() = SampleDocx.build(
        listOf(
            Block.Heading("Quarterly report"),
            Block.Paragraph("Sample document with a table and two charts. Switch \"Charts\" in the viewer to compare how they are shown."),
            Block.Table(
                listOf(
                    listOf("Quarter", "North", "South", "West"),
                    listOf("Q1", "4.3", "2.4", "2.0"),
                    listOf("Q2", "2.5", "4.4", "2.0"),
                    listOf("Q3", "3.5", "1.8", "3.0"),
                    listOf("Q4", "4.5", "2.8", "5.0")
                )
            ),
            Block.ChartBlock(
                Chart(
                    type = Chart.Type.COLUMN,
                    title = "Sales by quarter",
                    categories = listOf("Q1", "Q2", "Q3", "Q4"),
                    series = listOf(
                        "North" to listOf(4.3, 2.5, 3.5, 4.5),
                        "South" to listOf(2.4, 4.4, 1.8, 2.8),
                        "West" to listOf(2.0, 2.0, 3.0, 5.0)
                    )
                )
            ),
            Block.Paragraph("Text between the charts."),
            Block.ChartBlock(
                Chart(
                    type = Chart.Type.LINE,
                    title = "Monthly visitors",
                    categories = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun"),
                    series = listOf(
                        "2025" to listOf(120.0, 135.0, 150.0, 140.0, 170.0, 190.0),
                        "2026" to listOf(140.0, 160.0, 155.0, 180.0, 210.0, 230.0)
                    )
                )
            ),
            Block.Paragraph("End of the document.")
        )
    )

    private fun marketShare() = SampleDocx.build(
        listOf(
            Block.Heading("Market share"),
            Block.ChartBlock(
                Chart(
                    type = Chart.Type.PIE,
                    title = "Market share 2026",
                    categories = listOf("Product A", "Product B", "Product C", "Other"),
                    series = listOf("Share" to listOf(45.0, 25.0, 18.0, 12.0))
                )
            ),
            Block.ChartBlock(
                Chart(
                    type = Chart.Type.BAR,
                    title = "Customers by segment",
                    categories = listOf("Retail", "Business", "Government"),
                    series = listOf(
                        "2025" to listOf(320.0, 210.0, 80.0),
                        "2026" to listOf(380.0, 240.0, 95.0)
                    )
                )
            )
        )
    )

    private companion object {
        const val TAG = "SampleFiles"
        const val PREFERENCES = "sample_files"
        const val KEY_SEEDED = "seeded_v2"

        const val README = "Sample files created on the first launch.\nThey are temporary and will be removed before release.\n"
        const val BUDGET_CSV = "Item,January,February,March\nRent,1200,1200,1200\nSalaries,5400,5400,5600\nMarketing,800,950,700\nOther,300,250,400\n"
        const val REGIONS_CSV = "Region,2025,2026\nNorth,14.8,16.2\nSouth,11.4,12.9\nWest,9.0,11.5\n"
        const val RUSSIAN_TEXT = "Пример текста на русском языке.\nВторая строка: проверка определения кодировки.\n"
        const val NOTES = "# Notes\n\n- Markdown sample\n- Shown as plain text later\n"
    }
}
