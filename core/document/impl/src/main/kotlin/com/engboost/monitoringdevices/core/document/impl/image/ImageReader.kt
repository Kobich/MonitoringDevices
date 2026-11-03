package com.engboost.monitoringdevices.core.document.impl.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import com.engboost.monitoringdevices.core.document.api.DocumentFormat
import com.engboost.monitoringdevices.core.document.api.DocumentReadException
import com.engboost.monitoringdevices.core.document.api.DocumentReadException.Reason
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument
import com.engboost.monitoringdevices.core.document.impl.FormatReader
import com.engboost.monitoringdevices.core.document.impl.mapReadErrors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Images with `BitmapFactory`: only bounds are read on open, pixels are decoded
 * downsampled for the requested size. EXIF orientation is applied. GIF and animated WebP are
 * animated with `ImageDecoder` on Android 9+, older devices show the first frame.
 * HEIF needs Android 9+ as well.
 */
internal class ImageReader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FormatReader<ImageDocument> {
    override val formats = setOf(
        DocumentFormat.PNG,
        DocumentFormat.JPEG,
        DocumentFormat.GIF,
        DocumentFormat.WEBP,
        DocumentFormat.BMP,
        DocumentFormat.HEIF
    )

    override suspend fun read(file: File, format: DocumentFormat, password: String?): ImageDocument = withContext(ioDispatcher) {
        if (format == DocumentFormat.HEIF && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            throw DocumentReadException(Reason.UNSUPPORTED_FORMAT, "HEIF images need Android 9 or newer")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        readStream(file) { input -> BitmapFactory.decodeStream(input, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw DocumentReadException(Reason.CORRUPTED, "Can't read image ${file.name}")
        }

        val orientation = runCatching {
            readStream(file) { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val header = runCatching { readStream(file) { input -> input.readHeader() } }.getOrDefault(ByteArray(0))

        FileImageDocument(
            name = file.name,
            format = format,
            file = file,
            rawWidth = bounds.outWidth,
            rawHeight = bounds.outHeight,
            orientation = orientation,
            isAnimated = isGif(header) || isAnimatedWebp(header)
        )
    }

    private inner class FileImageDocument(
        override val name: String,
        override val format: DocumentFormat,
        private val file: File,
        private val rawWidth: Int,
        private val rawHeight: Int,
        private val orientation: Int,
        override val isAnimated: Boolean
    ) : ImageDocument {
        private val isRotatedSideways = orientation in SIDEWAYS_ORIENTATIONS
        override val widthPx: Int = if (isRotatedSideways) rawHeight else rawWidth
        override val heightPx: Int = if (isRotatedSideways) rawWidth else rawHeight

        override suspend fun decode(maxWidthPx: Int, maxHeightPx: Int): Bitmap = withContext(ioDispatcher) {
            mapReadErrors {
                // Limits apply to the displayed (rotated) image.
                val maxRawWidth = (if (isRotatedSideways) maxHeightPx else maxWidthPx).coerceAtLeast(1)
                val maxRawHeight = (if (isRotatedSideways) maxWidthPx else maxHeightPx).coerceAtLeast(1)

                val options = BitmapFactory.Options().apply {
                    inSampleSize = BitmapSampling.sampleSize(rawWidth, rawHeight, maxRawWidth, maxRawHeight)
                }
                val decoded = readStream(file) { input -> BitmapFactory.decodeStream(input, null, options) }
                    ?: throw DocumentReadException(Reason.CORRUPTED, "Can't decode image $name")

                transform(decoded, maxRawWidth, maxRawHeight)
            }
        }

        override suspend fun decodeAnimation(maxWidthPx: Int, maxHeightPx: Int): Drawable? {
            if (!isAnimated || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
            return withContext(ioDispatcher) {
                mapReadErrors { decodeAnimatedDrawable(maxWidthPx.coerceAtLeast(1), maxHeightPx.coerceAtLeast(1)) }
            }
        }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun decodeAnimatedDrawable(maxWidthPx: Int, maxHeightPx: Int): Drawable? {
            val drawable = try {
                ImageDecoder.decodeDrawable(ImageDecoder.createSource(file)) { decoder, info, _ ->
                    val scale = minOf(1f, maxWidthPx.toFloat() / info.size.width, maxHeightPx.toFloat() / info.size.height)
                    if (scale < 1f) {
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1)
                        )
                    }
                }
            } catch (error: IOException) {
                // ImageDecoder reports broken data as an IOException (DecodeException).
                throw DocumentReadException(Reason.CORRUPTED, error.message, error)
            }
            // A single-frame GIF decodes to a plain bitmap drawable: show it as a still image.
            return drawable.takeIf { it is AnimatedImageDrawable }
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

    private fun <T> readStream(file: File, block: (InputStream) -> T): T = file.inputStream().use(block)

    private fun InputStream.readHeader(): ByteArray {
        val header = ByteArray(HEADER_SIZE)
        var read = 0
        while (read < HEADER_SIZE) {
            val count = read(header, read, HEADER_SIZE - read)
            if (count < 0) break
            read += count
        }
        return header.copyOf(read)
    }

    private companion object {
        const val HEADER_SIZE = 32

        fun isGif(header: ByteArray): Boolean = header.startsWith("GIF8")

        /** RIFF....WEBPVP8X with the animation flag (0x02) in the VP8X flags byte. */
        fun isAnimatedWebp(header: ByteArray): Boolean {
            if (header.size < 21) return false
            return header.startsWith("RIFF") &&
                String(header, 8, 8, Charsets.US_ASCII) == "WEBPVP8X" &&
                header[20].toInt() and 0x02 != 0
        }

        private fun ByteArray.startsWith(prefix: String): Boolean {
            return size >= prefix.length && String(this, 0, prefix.length, Charsets.US_ASCII) == prefix
        }

        val SIDEWAYS_ORIENTATIONS = setOf(
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270
        )
    }
}
