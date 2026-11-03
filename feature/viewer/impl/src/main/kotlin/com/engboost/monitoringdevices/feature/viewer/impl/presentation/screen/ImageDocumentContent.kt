package com.engboost.monitoringdevices.feature.viewer.impl.presentation.screen

import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import com.engboost.monitoringdevices.core.commonui.components.MessageContent
import com.engboost.monitoringdevices.core.document.api.document.ImageDocument

/**
 * Fit to screen; pinch to zoom (up to 5x), drag to pan, double tap to toggle 2x / fit.
 * GIF and animated WebP play on Android 9+, otherwise their first frame is shown.
 */
@Composable
internal fun ImageDocumentContent(
    document: ImageDocument,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        // Decoded at twice the screen size so zooming in stays sharp.
        val maxWidthPx = with(density) { maxWidth.roundToPx() } * 2
        val maxHeightPx = with(density) { maxHeight.roundToPx() } * 2

        val result by produceState<Result<DecodedImage>?>(initialValue = null, document, maxWidthPx, maxHeightPx) {
            value = runCatching {
                val animation = if (document.isAnimated) document.decodeAnimation(maxWidthPx, maxHeightPx) else null
                if (animation != null) {
                    DecodedImage.Animated(animation)
                } else {
                    DecodedImage.Still(document.decode(maxWidthPx, maxHeightPx))
                }
            }
        }

        when (val decoded = result) {
            null -> CircularProgressIndicator()
            else -> decoded.fold(
                onSuccess = { image ->
                    ZoomableBox {
                        when (image) {
                            is DecodedImage.Still -> StillImage(image.bitmap)
                            is DecodedImage.Animated -> AnimatedImage(image.drawable)
                        }
                    }
                },
                onFailure = { MessageContent(title = "Can't show the image") }
            )
        }
    }
}

private sealed interface DecodedImage {
    class Still(val bitmap: Bitmap) : DecodedImage
    class Animated(val drawable: Drawable) : DecodedImage
}

@Composable
private fun StillImage(bitmap: Bitmap) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
        bitmap = imageBitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

/** `AnimatedImageDrawable` in an `ImageView`, which drives its frames. */
@Composable
private fun AnimatedImage(drawable: Drawable) {
    DisposableEffect(drawable) {
        (drawable as? Animatable)?.start()
        onDispose { (drawable as? Animatable)?.stop() }
    }
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                isClickable = false
            }
        },
        update = { view -> view.setImageDrawable(drawable) },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ZoomableBox(content: @Composable () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        fun clamp(value: Offset, zoom: Float): Offset {
            val maxX = width * (zoom - 1) / 2
            val maxY = height * (zoom - 1) / 2
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2f
                                // Zoom towards the tapped point.
                                offset = clamp(Offset(width / 2 - tap.x, height / 2 - tap.y), 2f)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                        offset = clamp(offset + pan, newScale)
                        scale = newScale
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            content()
        }
    }
}

private const val MAX_SCALE = 5f
