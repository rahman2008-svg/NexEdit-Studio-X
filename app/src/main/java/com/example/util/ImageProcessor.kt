package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.example.model.DrawStroke
import com.example.model.EditState
import com.example.model.FilterType
import java.io.InputStream

object ImageProcessor {

    // Load and downscale to safe dimensions to prevent OOM
    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1200): Bitmap? {
        return try {
            val contentResolver = context.contentResolver
            
            // Decode with inJustDecodeBounds to check dimensions
            var inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate exact scale factor
            var scale = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                scale = Math.pow(
                    2.0,
                    Math.ceil(
                        Math.log(
                            maxDimension.toDouble() / Math.max(options.outHeight, options.outWidth).toDouble()
                        ) / Math.log(0.5)
                    ).toInt().toDouble()
                ).toInt()
            }

            // Decode with actual sample size
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            inputStream = contentResolver.openInputStream(uri)
            val b = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()
            b
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Process pipeline
    fun processBitmap(
        base: Bitmap,
        state: EditState,
        blurRadius: Int = 0 // Blur radius slider (e.g., 0 to 15)
    ): Bitmap {
        var bmp = base

        // 1. Crop (applied first, non-destructive)
        if (state.cropLeft > 0f || state.cropTop > 0f || state.cropRight < 1f || state.cropBottom < 1f) {
            val leftPx = (state.cropLeft * bmp.width).toInt().coerceIn(0, bmp.width - 2)
            val topPx = (state.cropTop * bmp.height).toInt().coerceIn(0, bmp.height - 2)
            val rightPx = (state.cropRight * bmp.width).toInt().coerceIn(leftPx + 10, bmp.width)
            val bottomPx = (state.cropBottom * bmp.height).toInt().coerceIn(topPx + 10, bmp.height)
            try {
                bmp = Bitmap.createBitmap(bmp, leftPx, topPx, rightPx - leftPx, bottomPx - topPx)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Rotate / Flip operations
        if (state.rotation != 0f || state.isFlippedHorizontally || state.isFlippedVertically) {
            val matrix = android.graphics.Matrix()
            if (state.rotation != 0f) {
                matrix.postRotate(state.rotation)
            }
            val sx = if (state.isFlippedHorizontally) -1f else 1f
            val sy = if (state.isFlippedVertically) -1f else 1f
            if (sx != 1f || sy != 1f) {
                matrix.postScale(sx, sy)
            }
            try {
                val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                if (rotated != bmp && bmp != base) {
                    bmp.recycle()
                }
                bmp = rotated
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Color filtering + Brightness + Contrast + Saturation
        val colorMatrix = ColorMatrix()

        // Apply filters
        when (state.selectedFilter) {
            FilterType.VINTAGE -> {
                colorMatrix.set(floatArrayOf(
                    0.93f, 0f, 0f, 0f, 25f,
                    0f, 0.85f, 0f, 0f, 15f,
                    0f, 0f, 0.75f, 0f, -5f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterType.MONOCHROME -> {
                colorMatrix.setSaturation(0f)
                val highContrast = ColorMatrix(floatArrayOf(
                    1.25f, 0f, 0f, 0f, -20f,
                    0f, 1.25f, 0f, 0f, -20f,
                    0f, 0f, 1.25f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                ))
                colorMatrix.postConcat(highContrast)
            }
            FilterType.WARM_TONE -> {
                colorMatrix.set(floatArrayOf(
                    1.12f, 0f, 0f, 0f, 15f,
                    0f, 1.05f, 0f, 0f, 5f,
                    0f, 0f, 0.90f, 0f, -15f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterType.COOL_TONE -> {
                colorMatrix.set(floatArrayOf(
                    0.90f, 0f, 0f, 0f, -10f,
                    0f, 1.0f, 0f, 0f, 0f,
                    0f, 0f, 1.15f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            FilterType.GRAIN_DREAM -> {
                colorMatrix.set(floatArrayOf(
                    0.98f, 0f, 0f, 0f, 10f,
                    0f, 0.92f, 0f, 0f, 0f,
                    0f, 0f, 1.08f, 0f, 12f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            else -> {}
        }

        // Apply saturation
        if (state.saturation != 1f) {
            val satM = ColorMatrix().apply { setSaturation(state.saturation) }
            colorMatrix.postConcat(satM)
        }

        // Apply contrast
        if (state.contrast != 1f) {
            val scale = state.contrast
            val translate = (-0.5f * scale + 0.5f) * 255f
            val contrastM = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(contrastM)
        }

        // Apply brightness
        if (state.brightness != 0f) {
            val brightnessM = ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, state.brightness,
                0f, 1f, 0f, 0f, state.brightness,
                0f, 0f, 1f, 0f, state.brightness,
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(brightnessM)
        }

        // Render processed image
        var processed = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(processed)
        val paint = android.graphics.Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            isAntiAlias = true
        }
        canvas.drawBitmap(bmp, 0f, 0f, paint)

        // Recycle intermediate bitmap if it was cloned/created separately
        if (bmp != base && bmp != processed) {
            bmp.recycle()
        }

        // 4. Custom Local Filters (Sharpness Boost)
        if (state.selectedFilter == FilterType.SHARPNESS_BOOST) {
            val sharpened = sharpenBitmap(processed)
            processed.recycle()
            processed = sharpened
        }

        // 5. Box blur (convolution)
        if (blurRadius > 0) {
            val blurred = boxBlurBitmap(processed, blurRadius.coerceIn(1, 15))
            processed.recycle()
            processed = blurred
        }

        // 6. Drawing rasterization (using relative fraction coordinates for flawless scaling)
        if (state.strokes.isNotEmpty()) {
            val drawn = applyDrawingStrokes(processed, state.strokes)
            processed.recycle()
            processed = drawn
        }

        return processed
    }

    // Convolution sharpen filter
    private fun sharpenBitmap(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Kernel:
        // [ 0  -1   0 ]
        // [-1   5  -1 ]
        // [ 0  -1   0 ]
        for (y in 1 until height - 1) {
            val topOffset = (y - 1) * width
            val centerOffset = y * width
            val bottomOffset = (y + 1) * width

            for (x in 1 until width - 1) {
                val idx = centerOffset + x
                
                val center = pixels[idx]
                val top = pixels[topOffset + x]
                val bottom = pixels[bottomOffset + x]
                val left = pixels[idx - 1]
                val right = pixels[idx + 1]
                
                val rC = (center shr 16) and 0xFF
                val gC = (center shr 8) and 0xFF
                val bC = center and 0xFF
                
                val rT = (top shr 16) and 0xFF
                val gT = (top shr 8) and 0xFF
                val bT = top and 0xFF
                
                val rB = (bottom shr 16) and 0xFF
                val gB = (bottom shr 8) and 0xFF
                val bB = bottom and 0xFF
                
                val rL = (left shr 16) and 0xFF
                val gL = (left shr 8) and 0xFF
                val bL = left and 0xFF
                
                val rR = (right shr 16) and 0xFF
                val gR = (right shr 8) and 0xFF
                val bR = right and 0xFF
                
                var r = 5 * rC - rT - rB - rL - rR
                var g = 5 * gC - gT - gB - gL - gR
                var b = 5 * bC - bT - bB - bL - bR
                
                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)
                
                outPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        // Copy edges over in batch
        System.arraycopy(pixels, 0, outPixels, 0, width)
        System.arraycopy(pixels, (height - 1) * width, outPixels, (height - 1) * width, width)
        for (y in 0 until height) {
            outPixels[y * width] = pixels[y * width]
            outPixels[y * width + (width - 1)] = pixels[y * width + (width - 1)]
        }

        dest.setPixels(outPixels, 0, width, 0, 0, width, height)
        return dest
    }

    // Convolution box blur
    private fun boxBlurBitmap(src: Bitmap, radius: Int): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
        
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val outPixels = IntArray(width * height)

        // Horizontal blur pass
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0
                for (dx in -radius..radius) {
                    val nx = x + dx
                    if (nx in 0 until width) {
                        val p = pixels[rowOffset + nx]
                        rSum += (p shr 16) and 0xFF
                        gSum += (p shr 8) and 0xFF
                        bSum += p and 0xFF
                        count++
                    }
                }
                outPixels[rowOffset + x] = ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }

        // Vertical blur pass
        val tempPixels = outPixels.clone()
        for (x in 0 until width) {
            for (y in 0 until height) {
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0
                for (dy in -radius..radius) {
                    val ny = y + dy
                    if (ny in 0 until height) {
                        val p = tempPixels[ny * width + x]
                        rSum += (p shr 16) and 0xFF
                        gSum += (p shr 8) and 0xFF
                        bSum += p and 0xFF
                        count++
                    }
                }
                outPixels[y * width + x] = (0xFF shl 24) or ((rSum / count) shl 16) or ((gSum / count) shl 8) or (bSum / count)
            }
        }

        dest.setPixels(outPixels, 0, width, 0, 0, width, height)
        return dest
    }

    // Drawing Rasterizer using relative coordinates
    private fun applyDrawingStrokes(bitmap: Bitmap, strokes: List<DrawStroke>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(mutableBitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
            style = android.graphics.Paint.Style.STROKE
        }
        
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            paint.color = stroke.color.toInt()
            paint.strokeWidth = stroke.strokeWidth * (bitmap.width / 500f).coerceAtLeast(1f) // Scaled stroke width
            
            val path = android.graphics.Path()
            var isFirst = true
            for (offset in stroke.points) {
                val pxX = offset.x * bitmap.width
                val pxY = offset.y * bitmap.height
                if (isFirst) {
                    path.moveTo(pxX, pxY)
                    isFirst = false
                } else {
                    path.lineTo(pxX, pxY)
                }
            }
            canvas.drawPath(path, paint)
        }
        return mutableBitmap
    }
}
