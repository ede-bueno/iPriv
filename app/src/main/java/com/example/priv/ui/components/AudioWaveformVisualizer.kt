package com.example.priv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformVisualizer(
    waveformData: String,
    isPlaying: Boolean,
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    barWidthDp: Float = 4f,
    barSpacingDp: Float = 3f,
    onSeek: (Float) -> Unit = {}
) {
    val sampleHeights = remember(waveformData) {
        val parsed = waveformData.split(",")
            .mapNotNull { it.trim().toFloatOrNull() }
            .filter { it in 0.0f..1.0f }
        if (parsed.isNotEmpty()) parsed else listOf(0.3f, 0.6f, 0.8f, 0.4f, 0.9f, 0.5f, 0.7f, 0.2f, 0.6f, 0.8f, 0.3f, 0.7f, 0.9f, 0.4f, 0.6f)
    }

    // Gentle pulse animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulse")
    val pulseFactor by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                }
            }
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        if (totalWidth <= 0f || maxHeight <= 0f) return@Canvas

        val barCount = sampleHeights.size
        val barWidthPx = barWidthDp.dp.toPx()
        val spacingPx = barSpacingDp.dp.toPx()

        // Calculate actual bar width to fit canvas if needed
        val computedStep = totalWidth / barCount.coerceAtLeast(1)
        val minBarHeight = minOf(6f, maxHeight)

        for (i in 0 until barCount) {
            val sample = sampleHeights[i]
            val active = (i.toFloat() / barCount) <= progress

            val dynamicFactor = if (isPlaying && active) pulseFactor else 1.0f
            val barHeight = (maxHeight * sample * 0.85f * dynamicFactor).coerceIn(minBarHeight, maxHeight)

            val x = i * computedStep + (computedStep - barWidthPx) / 2f
            val y = (maxHeight - barHeight) / 2f

            val color = if (active) activeColor else inactiveColor

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidthPx.coerceAtMost(computedStep), barHeight),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
            )
        }
    }
}
