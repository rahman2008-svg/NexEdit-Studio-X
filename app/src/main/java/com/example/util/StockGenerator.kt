package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.geometry.Offset

object StockGenerator {

    enum class StockTheme {
        NEON_CYBER,
        AUTUMN_MOUNTAIN,
        COSMIC_AURORA,
        MODERN_BRUTALIST
    }

    fun generateStockBitmap(theme: StockTheme, width: Int = 1000, height: Int = 1000): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = true }

        when (theme) {
            StockTheme.NEON_CYBER -> {
                // Background gradient (Dark deep blue-violet to dark magenta)
                val bgGradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    0xFF0A0214.toInt(), 0xFF3B0745.toInt(),
                    Shader.TileMode.CLAMP
                )
                paint.shader = bgGradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Drawing neon wireframe sun
                val sunColor = LinearGradient(
                    0f, height * 0.2f, 0f, height * 0.7f,
                    0xFFFF007F.toInt(), 0xFFFFCC00.toInt(),
                    Shader.TileMode.CLAMP
                )
                paint.shader = sunColor
                canvas.drawCircle(width * 0.5f, height * 0.5f, width * 0.25f, paint)
                paint.shader = null

                // Draw neon laser perspective block grid lines
                paint.color = 0x8800FFFF.toInt() // Neon Cyan
                paint.strokeWidth = 3f
                paint.style = Paint.Style.STROKE
                val horizonY = height * 0.55f
                
                // Horizontal lines with exponential distance
                var dy = 0f
                for (i in 0..10) {
                    val progress = i / 10f
                    val lineY = horizonY + (height - horizonY) * progress * progress
                    canvas.drawLine(0f, lineY, width.toFloat(), lineY, paint)
                }
                
                // Perspective vertical radial spokes
                for (i in 0..12) {
                    val progress = i / 12f
                    val xStart = width * progress
                    canvas.drawLine(xStart, height.toFloat(), width * 0.5f, horizonY, paint)
                }
            }

            StockTheme.AUTUMN_MOUNTAIN -> {
                // Background gradient (Warm Golden hour)
                val bgGradient = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    0xFFFC5C7D.toInt(), 0xFF6A82FB.toInt(),
                    Shader.TileMode.CLAMP
                )
                paint.shader = bgGradient
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                paint.shader = null

                // Draw mountains using polygon paths
                paint.style = Paint.Style.FILL

                // Back Mountain Range (Dusty Purple)
                paint.color = 0xFF5B4573.toInt()
                val path1 = Path().apply {
                    moveTo(0f, height * 0.7f)
                    lineTo(width * 0.35f, height * 0.45f)
                    lineTo(width * 0.65f, height * 0.65f)
                    lineTo(width * 0.85f, height * 0.35f)
                    lineTo(width.toFloat(), height * 0.6f)
                    lineTo(width.toFloat(), height.toFloat())
                    lineTo(0f, height.toFloat())
                    close()
                }
                canvas.drawPath(path1, paint)

                // Middle Mountain Range (Teal Forest)
                paint.color = 0xFF235359.toInt()
                val path2 = Path().apply {
                    moveTo(0f, height * 0.8f)
                    lineTo(width * 0.2f, height * 0.58f)
                    lineTo(width * 0.5f, height * 0.75f)
                    lineTo(width * 0.75f, height * 0.52f)
                    lineTo(width.toFloat(), height * 0.85f)
                    lineTo(width.toFloat(), height.toFloat())
                    lineTo(0f, height.toFloat())
                    close()
                }
                canvas.drawPath(path2, paint)

                // Foreground Pine Trees (Simple silhouette triangles)
                paint.color = 0xFF122C34.toInt()
                canvas.drawRect(0f, height * 0.88f, width.toFloat(), height.toFloat(), paint)
                
                // Draw 5 trees at random offsets
                val treeCenters = listOf(width * 0.15f, width * 0.35f, width * 0.55f, width * 0.72f, width * 0.88f)
                val treeHeights = listOf(140f, 180f, 150f, 210f, 160f)
                for (i in treeCenters.indices) {
                    val cx = treeCenters[i]
                    val th = treeHeights[i]
                    val bottomY = height * 0.9f
                    val topY = bottomY - th
                    
                    val treePath = Path().apply {
                        moveTo(cx, topY)
                        lineTo(cx - 35f, bottomY)
                        lineTo(cx + 35f, bottomY)
                        close()
                    }
                    canvas.drawPath(treePath, paint)
                }
            }

            StockTheme.COSMIC_AURORA -> {
                // Background dark navy
                paint.color = 0xFF03001E.toInt()
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Aurora light glow with transparent path gradients
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 90f
                paint.strokeCap = Paint.Cap.ROUND
                
                // Dynamic aurora curves
                // Emerald curve
                paint.color = 0x4400FF87.toInt()
                val auroraPath1 = Path().apply {
                    moveTo(0f, height * 0.4f)
                    cubicTo(width * 0.25f, height * 0.2f, width * 0.7f, height * 0.8f, width.toFloat(), height * 0.35f)
                }
                canvas.drawPath(auroraPath1, paint)

                // Violet glow curve
                paint.color = 0x3360EFFF.toInt()
                paint.strokeWidth = 140f
                val auroraPath2 = Path().apply {
                    moveTo(0f, height * 0.48f)
                    cubicTo(width * 0.3f, height * 0.55f, width * 0.6f, height * 0.1f, width.toFloat(), height * 0.42f)
                }
                canvas.drawPath(auroraPath2, paint)

                // Draw tiny warm stars
                paint.style = Paint.Style.FILL
                val stars = listOf(
                    Offset(100f, 150f), Offset(180f, 320f), Offset(280f, 110f),
                    Offset(440f, 250f), Offset(650f, 140f), Offset(720f, 350f),
                    Offset(850f, 200f), Offset(910f, 80f), Offset(50f, 500f)
                )
                for (star in stars) {
                    paint.color = 0xCCFFFFFF.toInt()
                    canvas.drawCircle(star.x, star.y, 4f, paint)
                    // Light outer halo
                    paint.color = 0x22FFFFFF.toInt()
                    canvas.drawCircle(star.x, star.y, 10f, paint)
                }
            }

            StockTheme.MODERN_BRUTALIST -> {
                // Background (Clean Warm Cream)
                paint.color = 0xFFEBE3D5.toInt()
                paint.style = Paint.Style.FILL
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Drawing modern vector shapes with contrasting bold colors
                paint.style = Paint.Style.FILL

                // Huge terracotta red quarter-circle
                paint.color = 0xFFB04A3C.toInt()
                canvas.drawArc(
                    -width * 0.2f, -height * 0.2f,
                    width * 0.7f, height * 0.7f,
                    0f, 360f, true, paint
                )

                // Cobalt Blue rectangle
                paint.color = 0xFF142F6B.toInt()
                canvas.drawRect(width * 0.4f, height * 0.45f, width * 0.85f, height * 0.75f, paint)

                // Forest Green triangle
                paint.color = 0xFF2A4D30.toInt()
                val triPath = Path().apply {
                    moveTo(width * 0.15f, height * 0.85f)
                    lineTo(width * 0.45f, height * 0.55f)
                    lineTo(width * 0.55f, height * 0.85f)
                    close()
                }
                canvas.drawPath(triPath, paint)

                // Charcoal lines and small yellow orb
                paint.color = 0xFF222222.toInt()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                canvas.drawCircle(width * 0.75f, height * 0.3f, 40f, paint)

                paint.color = 0xFFF0A500.toInt() // Mustard Yellow
                paint.style = Paint.Style.FILL
                canvas.drawCircle(width * 0.75f, height * 0.3f, 34f, paint)

                // Fine abstract geometric lines
                paint.color = 0xFF222222.toInt()
                paint.strokeWidth = 4f
                canvas.drawLine(width * 0.1f, height * 0.2f, width * 0.9f, height * 0.2f, paint)
                canvas.drawLine(width * 0.25f, height * 0.15f, width * 0.25f, height * 0.45f, paint)
            }
        }

        return bitmap
    }
}
