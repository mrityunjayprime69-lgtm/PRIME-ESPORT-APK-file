package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.media.ExifInterface
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure Kotlin/Android Bitmap preview & crop dialog supporting both circular avatars (1:1)
 * and wide banners (16:6) with pinch/drag/zoom slider adjustments and zero native ABI risk.
 */
@Composable
fun AvatarCropDialog(
    sourceUri: Uri,
    isCircleAvatar: Boolean = true,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadError by remember(sourceUri) { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var zoom by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(sourceUri) {
        val loaded = withContext(Dispatchers.IO) {
            loadOrientedBitmap(context, sourceUri, maxDim = 1400)
        }
        if (loaded == null) {
            loadError = "Unable to decode selected image."
        } else {
            sourceBitmap = loaded
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = PrimeNavySurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isCircleAvatar) "Adjust Profile Photo" else "Adjust Profile Banner",
                    color = PrimeTextPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = "Pinch or drag to frame your photo",
                    color = PrimeTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (isCircleAvatar) 1f else 16f / 7f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .clipToBounds()
                        .onSizeChanged { boxSize = it }
                        .pointerInput(sourceBitmap) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                val newZoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                zoom = newZoom
                                val maxOffsetX = (boxSize.width * (newZoom - 1f)) / 2f + boxSize.width * 0.25f
                                val maxOffsetY = (boxSize.height * (newZoom - 1f)) / 2f + boxSize.height * 0.25f
                                offset = Offset(
                                    x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                    y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = sourceBitmap
                    when {
                        loadError != null -> {
                            Text(loadError ?: "", color = PrimeRed, fontSize = 13.sp)
                        }
                        bmp == null -> {
                            CircularProgressIndicator(color = PrimeBlue)
                        }
                        else -> {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Crop preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoom,
                                        scaleY = zoom,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )

                            ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                                val overlayPath = Path().apply {
                                    fillType = PathFillType.EvenOdd
                                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                                    if (isCircleAvatar) {
                                        val r = minOf(size.width, size.height) * 0.44f
                                        addOval(
                                            androidx.compose.ui.geometry.Rect(
                                                left = size.width / 2f - r,
                                                top = size.height / 2f - r,
                                                right = size.width / 2f + r,
                                                bottom = size.height / 2f + r
                                            )
                                        )
                                    } else {
                                        val pad = 12.dp.toPx()
                                        addRect(
                                            androidx.compose.ui.geometry.Rect(
                                                pad,
                                                pad,
                                                size.width - pad,
                                                size.height - pad
                                            )
                                        )
                                    }
                                }
                                drawPath(overlayPath, color = Color.Black.copy(alpha = 0.55f))
                                if (isCircleAvatar) {
                                    val r = minOf(size.width, size.height) * 0.44f
                                    drawCircle(
                                        color = PrimeBlue,
                                        radius = r,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                } else {
                                    val pad = 12.dp.toPx()
                                    drawRect(
                                        color = PrimeBlue,
                                        topLeft = Offset(pad, pad),
                                        size = androidx.compose.ui.geometry.Size(
                                            size.width - pad * 2f,
                                            size.height - pad * 2f
                                        ),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zoom", color = PrimeTextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it },
                        valueRange = 1f..4f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimeBlue,
                            activeTrackColor = PrimeBlue
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = PrimeTextPrimary)
                    }
                    Button(
                        onClick = {
                            val bmp = sourceBitmap ?: return@Button
                            if (boxSize.width <= 0 || boxSize.height <= 0) return@Button
                            isSaving = true
                            scope.launch {
                                val outUri = withContext(Dispatchers.IO) {
                                    renderCroppedBitmapToCache(
                                        context = context,
                                        bitmap = bmp,
                                        viewportSize = boxSize,
                                        zoom = zoom,
                                        offset = offset,
                                        outputWidth = if (isCircleAvatar) 512 else 1280,
                                        outputHeight = if (isCircleAvatar) 512 else 560
                                    )
                                }
                                isSaving = false
                                if (outUri != null) {
                                    onCropped(outUri)
                                } else {
                                    loadError = "Failed to save cropped photo."
                                }
                            }
                        },
                        enabled = !isSaving && sourceBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Use Photo", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

private fun loadOrientedBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim * 2 || bounds.outHeight / sampleSize > maxDim * 2) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return null

        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return raw
        }
        Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    } catch (_: Exception) {
        null
    }
}

private fun renderCroppedBitmapToCache(
    context: Context,
    bitmap: Bitmap,
    viewportSize: IntSize,
    zoom: Float,
    offset: Offset,
    outputWidth: Int,
    outputHeight: Int
): Uri? {
    return try {
        val vw = viewportSize.width.toFloat().coerceAtLeast(1f)
        val vh = viewportSize.height.toFloat().coerceAtLeast(1f)
        val baseScale = max(vw / bitmap.width.toFloat(), vh / bitmap.height.toFloat())
        val totalScale = baseScale * zoom

        val drawnW = bitmap.width * totalScale
        val drawnH = bitmap.height * totalScale
        val left = (vw - drawnW) / 2f + offset.x
        val top = (vh - drawnH) / 2f + offset.y

        val cropLeftPx = ((-left) / totalScale).roundToInt().coerceIn(0, bitmap.width - 1)
        val cropTopPx = ((-top) / totalScale).roundToInt().coerceIn(0, bitmap.height - 1)
        val cropRightPx = (((vw - left) / totalScale)).roundToInt().coerceIn(cropLeftPx + 1, bitmap.width)
        val cropBottomPx = (((vh - top) / totalScale)).roundToInt().coerceIn(cropTopPx + 1, bitmap.height)

        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(android.graphics.Color.BLACK)
        canvas.drawBitmap(
            bitmap,
            Rect(cropLeftPx, cropTopPx, cropRightPx, cropBottomPx),
            Rect(0, 0, outputWidth, outputHeight),
            null
        )

        val outFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { fos ->
            output.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        Uri.fromFile(outFile)
    } catch (_: Exception) {
        null
    }
}
