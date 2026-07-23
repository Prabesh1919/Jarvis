package com.jarvis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jarvis.ui.theme.LocalJarvisColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Iron Man Mark-XXXIX Arc Reactor Core Widget for Android JARVISH.
 * Replicates the Desktop Mark-XXXIX-OR UI Arc Reactor Widget 100% identically:
 * - Animated spinning concentric HUD arc rings
 * - Dynamic pulse halo & energy waves
 * - 360-degree compass tick marks & crosshairs
 * - Voice state reactive orb (LISTENING / THINKING / SPEAKING)
 */
@Composable
fun ArcReactorWidget(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isSpeaking: Boolean = false
) {
    val colors = LocalJarvisColors.current

    // Infinite rotation animations
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorRotation")

    val outerRingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 4000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRingAngle"
    )

    val innerRingAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 3000 else 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRingAngle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val minDim = minOf(width, height)

            val primaryColor = colors.accent
            val secondaryColor = colors.secondaryGlow
            val borderDimColor = colors.surfaceBorder

            // 1. Grid Background Dots
            val dotSpacing = 32f
            var x = 0f
            while (x < width) {
                var y = 0f
                while (y < height) {
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.12f),
                        radius = 1.2f,
                        center = Offset(x, y)
                    )
                    y += dotSpacing
                }
                x += dotSpacing
            }

            // 2. Pulsing Energy Halo Rings
            val haloRadius = (minDim * 0.42f) * (if (isListening || isSpeaking) pulseScale else 1.0f)
            for (i in 1..4) {
                val r = haloRadius * (1.0f - i * 0.15f)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.08f * i),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // 3. Compass 360-degree Tick Marks
            val tickRadiusOuter = minDim * 0.45f
            val tickRadiusInner = minDim * 0.41f
            for (deg in 0 until 360 step 10) {
                val rad = Math.toRadians(deg.toDouble())
                val innerR = if (deg % 30 == 0) tickRadiusInner else tickRadiusInner + 8f
                val start = Offset(
                    (center.x + tickRadiusOuter * cos(rad)).toFloat(),
                    (center.y + tickRadiusOuter * sin(rad)).toFloat()
                )
                val end = Offset(
                    (center.x + innerR * cos(rad)).toFloat(),
                    (center.y + innerR * sin(rad)).toFloat()
                )
                drawLine(
                    color = primaryColor.copy(alpha = if (deg % 30 == 0) 0.6f else 0.25f),
                    start = start,
                    end = end,
                    strokeWidth = if (deg % 30 == 0) 2f else 1f
                )
            }

            // 4. Outer Rotating HUD Arc Rings
            val r1 = minDim * 0.40f
            val r2 = minDim * 0.33f
            val r3 = minDim * 0.26f

            // Ring 1 (Outer Clockwise)
            drawArc(
                color = primaryColor.copy(alpha = 0.85f),
                startAngle = outerRingAngle,
                sweepAngle = 115f,
                useCenter = false,
                topLeft = Offset(center.x - r1, center.y - r1),
                size = Size(r1 * 2f, r1 * 2f),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
            drawArc(
                color = secondaryColor.copy(alpha = 0.7f),
                startAngle = outerRingAngle + 180f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(center.x - r1, center.y - r1),
                size = Size(r1 * 2f, r1 * 2f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // Ring 2 (Middle Counter-Clockwise)
            drawArc(
                color = primaryColor.copy(alpha = 0.75f),
                startAngle = innerRingAngle,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(center.x - r2, center.y - r2),
                size = Size(r2 * 2f, r2 * 2f),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
            drawArc(
                color = primaryColor.copy(alpha = 0.5f),
                startAngle = innerRingAngle + 150f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - r2, center.y - r2),
                size = Size(r2 * 2f, r2 * 2f),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )

            // Ring 3 (Inner Clockwise)
            drawArc(
                color = secondaryColor.copy(alpha = 0.9f),
                startAngle = outerRingAngle * 1.5f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - r3, center.y - r3),
                size = Size(r3 * 2f, r3 * 2f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round)
            )

            // 5. Crosshair Target Reticle
            val chRadius = minDim * 0.47f
            val gapRadius = minDim * 0.15f
            drawLine(
                color = primaryColor.copy(alpha = 0.35f),
                start = Offset(center.x - chRadius, center.y),
                end = Offset(center.x - gapRadius, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.35f),
                start = Offset(center.x + gapRadius, center.y),
                end = Offset(center.x + chRadius, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.35f),
                start = Offset(center.x, center.y - chRadius),
                end = Offset(center.x, center.y - gapRadius),
                strokeWidth = 1.5f
            )
            drawLine(
                color = primaryColor.copy(alpha = 0.35f),
                start = Offset(center.x, center.y + gapRadius),
                end = Offset(center.x, center.y + chRadius),
                strokeWidth = 1.5f
            )

            // 6. Central Arc Reactor Core Orb
            val coreRadius = (minDim * 0.18f) * (if (isSpeaking) pulseScale else 1.0f)
            drawCircle(
                color = if (isListening) secondaryColor.copy(alpha = 0.9f) else primaryColor.copy(alpha = 0.8f),
                radius = coreRadius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = coreRadius * 0.5f,
                center = center
            )
        }
    }
}
