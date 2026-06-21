package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WheelOption

@Composable
fun WheelComponent(
    options: List<WheelOption>,
    targetAngle: Float,
    isSpinning: Boolean,
    onAnimationEnd: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val animatableRotation = remember { Animatable(0f) }

    LaunchedEffect(targetAngle) {
        if (targetAngle > 0f) {
            animatableRotation.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(
                    durationMillis = 4200,
                    easing = CubicBezierEasing(0.12f, 0.93f, 0.22f, 1f) // Ultra-smooth realistic decay
                )
            )
            // Animation complete
            val optionsCount = options.size
            if (optionsCount > 0) {
                val sliceAngle = 360f / optionsCount
                val finalAngleMod = (targetAngle % 360f)
                val middleAngle = (270f - finalAngleMod + 360f) % 360f
                val index = (((middleAngle / sliceAngle)) % optionsCount).toInt()
                onAnimationEnd(index)
            }
        } else if (targetAngle == 0f) {
            animatableRotation.snapTo(0f)
        }
    }

    val currentRotation = animatableRotation.value

    // Perform highly realistic tactile peg-collision clicks based on rotation
    if (options.isNotEmpty()) {
        val sliceAngle = 360f / options.size
        // Whenever the rotation crosses integer multiples of slot borders, trigger a vibration!
        val rawIndex = (currentRotation / sliceAngle).toInt()
        LaunchedEffect(rawIndex) {
            if (isSpinning) {
                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) // Light physical tick
            }
        }
    }

    Box(modifier = modifier.aspectRatio(1f)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val outerRadius = (width.coerceAtMost(height) / 2f) - 12.dp.toPx()
            val innerRadius = outerRadius - 10.dp.toPx()

            // 1. Draw elegant outer rim (metallic slate ring)
            drawCircle(
                color = Color(0xFF1E252B),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF3F4E5A),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Draw peg markers on outer frame for visual feedback
            if (options.isNotEmpty()) {
                val numSlices = options.size
                for (i in 0 until numSlices) {
                    val pegAngle = i * (360f / numSlices)
                    rotate(pegAngle, pivot = center) {
                        drawCircle(
                            color = Color(0xFFEEEEEE),
                            radius = 4.dp.toPx(),
                            center = Offset(center.x, center.y - outerRadius + 6.dp.toPx())
                        )
                    }
                }
            }

            // 2. Draw active rotating elements of the wheel
            rotate(currentRotation, pivot = center) {
                if (options.isNotEmpty()) {
                    val sliceAngle = 360f / options.size
                    val wheelRect = Size(innerRadius * 2, innerRadius * 2)
                    val wheelTopLeft = Offset(center.x - innerRadius, center.y - innerRadius)

                    for (i in options.indices) {
                        val option = options[i]
                        val startAngle = i * sliceAngle
                        val color = try {
                            Color(android.graphics.Color.parseColor(option.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }

                        // Draw filled slice colored arc
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sliceAngle,
                            useCenter = true,
                            topLeft = wheelTopLeft,
                            size = wheelRect
                        )

                        // Draw subtle inner slice border
                        drawArc(
                            color = Color(0x33000000),
                            startAngle = startAngle,
                            sweepAngle = sliceAngle,
                            useCenter = true,
                            topLeft = wheelTopLeft,
                            size = wheelRect,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // 3. Draw text label rotated perfectly along bisector
                        val textAngle = startAngle + (sliceAngle / 2f)
                        val truncatedLabel = if (option.label.length > 12) {
                            option.label.take(10) + "..."
                        } else {
                            option.label
                        }

                        val isLight = isColorLight(option.colorHex)
                        val textColor = if (isLight) android.graphics.Color.BLACK else android.graphics.Color.WHITE

                        val paint = android.graphics.Paint().apply {
                            this.color = textColor
                            this.textSize = with(density) { 13.sp.toPx() }
                            this.typeface = android.graphics.Typeface.DEFAULT_BOLD
                            this.textAlign = android.graphics.Paint.Align.RIGHT
                            this.isAntiAlias = true
                        }

                        rotate(textAngle, pivot = center) {
                            // Align text nicely from the outer edge pointing inwards
                            val drawX = center.x + innerRadius - 16.dp.toPx()
                            val drawY = center.y + (paint.textSize / 3f)
                            drawContext.canvas.nativeCanvas.drawText(
                                truncatedLabel,
                                drawX,
                                drawY,
                                paint
                            )
                        }
                    }
                } else {
                    // Empty state helper wheel display
                    drawCircle(
                        color = Color(0x1F7C4DFF),
                        radius = innerRadius,
                        center = center
                    )
                }
            }

            // 4. Draw Center Pin cover
            drawCircle(
                color = Color(0xFF111111),
                radius = 20.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF888888),
                radius = 16.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF222222),
                radius = 12.dp.toPx(),
                center = center
            )

            // 5. Draw Stationary Pointer Arrow at top (270 degrees)
            // Arrow triangle pointing directly downwards onto outer rim
            val arrowLength = 22.dp.toPx()
            val arrowWidth = 16.dp.toPx()
            val arrowTopY = center.y - outerRadius - 12.dp.toPx()

            val path = Path().apply {
                moveTo(center.x, arrowTopY + arrowLength) // Tip of arrow pointing down
                lineTo(center.x - arrowWidth / 2f, arrowTopY) // Left top
                lineTo(center.x + arrowWidth / 2f, arrowTopY) // Right top
                close()
            }

            // Draw shadow shadow
            drawPath(
                path = path,
                color = Color(0x66000000)
            )
            // Draw actual pointer needle (Cherry Crimson color)
            drawPath(
                path = path,
                color = Color(0xFFFF2A6D)
            )
            // Stroke of arrow needle
            drawPath(
                path = path,
                color = Color(0xFFFFFFFF),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

// Check Color Luma for accessibility contrast
private fun isColorLight(colorHex: String): Boolean {
    return try {
        val color = android.graphics.Color.parseColor(colorHex)
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        val luma = 0.299f * r + 0.587f * g + 0.114f * b
        luma > 175f
    } catch (e: Exception) {
        false
    }
}
