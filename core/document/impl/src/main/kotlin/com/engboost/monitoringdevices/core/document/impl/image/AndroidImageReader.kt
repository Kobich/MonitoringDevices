package com.engboost.monitoringdevices.core.document.impl.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.DocumentSource
import com.engboost.monitoringdevices.core.document.api.ReadOptions
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.api.reader.image.ImageReader
import com.engboost.monitoringdevices.core.document.impl.source.DocumentSourceFiles
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Still images with `BitmapFactory`: only bounds are read on open, pixels are decoded
 * downsampled for the requested size. EXIF orientation is applied. Animated GIF/WebP show
 * their first frame (an image library such as Coil can take over later).
 */
internal class AndroidImageReader(
    private val sourceFiles: DocumentSourceFiles,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImageReader {

    override suspend fun read(source: DocumentSource, options: ReadOptions): ImageDocument = withContext(ioDispatcher) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        readStream(source) { input -> BitmapFactory.decodeStream(input, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw DocumentReadException(Reason.CORRUPTED, "Can't read image ${source.name}")
        }

        val orientation = runCatching {
            readStream(source) { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        AndroidImageDocument(
            name = source.name,
            source = source,
            rawWidth = bounds.outWidth,
            rawHeight = bounds.outHeight,
            orientation = orientation
        )
    }

    private inner class AndroidImageDocument(
        override val name: String,
        private val source: DocumentSource,
        private val rawWidth: Int,
        private val rawHeight: Int,
        private val orientation: Int
    ) : ImageDocument {
        override val format: DocumentFormat = DocumentFormat.IMAGE

        private val isRotatedSideways = orientation in SIDEWAYS_ORIENTATIONS
        override val widthPx: Int = if (isRotatedSideways) rawHeight else rawWidth
        override val heightPx: Int = if (isRotatedSideways) rawWidth else rawHeight

        override suspend fun decode(maxWidthPx: Int, maxHeightPx: Int): Bitmap = withContext(ioDispatcher) {
            // Limits apply to the displayed (rotated) image.
            val maxRawWidth = if (isRotatedSideways) maxHeightPx else maxWidthPx
            val maxRawHeight = if (isRotatedSideways) maxWidthPx else maxHeightPx

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(rawWidth, rawHeight, maxRawWidth, maxRawHeight)
            }
            val decoded = readStream(source) { input -> BitmapFactory.decodeStream(input, null, options) }
                ?: throw DocumentReadException(Reason.CORRUPTED, "Can't decode image $name")

            transform(decoded, maxRawWidth, maxRawHeight)
        }

        /** Exact downscale to the limits plus EXIF rotation/mirroring, in one pass. */
        private fun transform(bitmap: Bitmap, maxRawWidth: Int, maxRawHeight: Int): Bitmap {
            val scale = minOf(1f, maxRawWidth.toFloat() / bitmap.width, maxRawHeight.toFloat() / bitmap.height)
            val matrix = Matrix().apply {
                postScale(scale, scale)
                when (orientation) {
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        postRotate(90f)
                        postScale(-1f, 1f)
                    }

                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        postRotate(-90f)
                        postScale(-1f, 1f)
                    }

                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(-90f)
                }
            }
            if (matrix.isIdentity) return bitmap

            val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (result !== bitmap) bitmap.recycle()
            return result
        }
    }

    private fun <T> readStream(source: DocumentSource, block: (java.io.InputStream) -> T): T {
        return try {
            sourceFiles.openInputStream(source).use(block)
        } catch (error: DocumentReadException) {
            throw error
        } catch (error: IOException) {
            throw DocumentReadException(Reason.IO_ERROR, error.message, error)
        } catch (error: OutOfMemoryError) {
            throw DocumentReadException(Reason.TOO_LARGE, "Not enough memory for the image", error)
        }
    }

    private companion object {
        val SIDEWAYS_ORIENTATIONS = setOf(
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270
        )

        /** Largest power of two that keeps the decoded image at least as big as the limits. */
        fun sampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
            var sample = 1
            while (width / (sample * 2) >= maxWidth && height / (sample * 2) >= maxHeight) {
                sample *= 2
            }
            return sample
        }
    }
}
