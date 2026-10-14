package com.example.priv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.ui.audio.AudioPlaybackState
import com.example.priv.ui.audio.AudioPlayerManager
import kotlin.math.sin

/**
 * Visual waveform rendering styles.
 */
enum class WaveformStyle {
    /** Waveform bars centered vertically, expanding symmetrically top and bottom */
    SYMMETRICAL,
    /** Waveform bars anchored at the bottom baseline */
    BOTTOM_ALIGNED,
    /** Waveform bars with mirrored reflection with lower amplitude underneath */
    REFLECTIVE
}

/**
 * Custom UI Component to display a visual audio waveform during the playback
 * of recorded memories, providing a responsive and engaging user experience.
 *
 * Features:
 * - Dynamic live frequency ripple and harmonic oscillations when audio is playing.
 * - Interactive tap-to-seek and drag-to-scrub with live playhead tracking.
 * - Glowing playhead cursor needle with rounded cap.
 * - Floating scrub time tooltip showing exact position during drag.
 * - Responsive sample resampling adapting smoothly to any screen width.
 * - Customizable gradient brushes, bar styles, and animation effects.
 */
@Composable
fun PlaybackWaveformVisualizer(
    waveformData: String,
    isPlaying: Boolean,
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    totalDurationMs: Long = 0L,
    style: WaveformStyle = WaveformStyle.SYMMETRICAL,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    activeSecondaryColor: Color = MaterialTheme.colorScheme.tertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    playheadColor: Color = MaterialTheme.colorScheme.primary,
    showPlayhead: Boolean = true,
    showScrubTooltip: Boolean = true,
    enableSeeking: Boolean = true,
    barWidthDp: Float = 3.5f,
    barSpacingDp: Float = 2.5f,
    minBarHeightDp: Float = 4f,
    onSeek: (Float) -> Unit = {}
) {
    // Parse and normalize raw waveform data (supports 0.0-1.0 floats, 0-100 integers, or empty)
    val parsedSamples = remember(waveformData) {
        parseWaveformSamples(waveformData)
    }

    // State for interactive dragging & scrubbing preview
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val effectiveProgress = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    // Animated ripple phase for live playback dynamics
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_live_dynamics")

    // Traveling wave ripple phase
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Gentle amplitude bounce for active bars
    val amplitudeBounce by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "amplitude_bounce"
    )

    // Glow pulse for the playhead cursor
    val playheadGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "playhead_glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("playback_waveform_visualizer"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Scrubbing Floating Tooltip (shown when actively dragging)
            AnimatedVisibility(
                visible = isDragging && showScrubTooltip && totalDurationMs > 0,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                        .testTag("waveform_scrub_tooltip")
                ) {
                    val scrubMs = (totalDurationMs * dragFraction).toLong().coerceAtLeast(0L)
                    val mins = (scrubMs / 1000) / 60
                    val secs = (scrubMs / 1000) % 60
                    Text(
                        text = String.format("%02d:%02d", mins, secs),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Canvas Waveform Drawing
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .fillMaxHeight()
                    .pointerInput(enableSeeking) {
                        if (!enableSeeking) return@pointerInput
                        detectTapGestures { offset ->
                            if (size.width > 0) {
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onSeek(fraction)
                            }
                        }
                    }
                    .pointerInput(enableSeeking) {
                        if (!enableSeeking) return@pointerInput
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                isDragging = false
                                onSeek(dragFraction)
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                onSeek(dragFraction)
                            }
                        )
                    }
            ) {
                val totalWidth = size.width
                val maxHeight = size.height
                if (totalWidth <= 0f || maxHeight <= 0f) return@Canvas

                val barWidthPx = barWidthDp.dp.toPx()
                val barSpacingPx = barSpacingDp.dp.toPx()
                val minBarHeightPx = minBarHeightDp.dp.toPx()

                // Calculate optimal bar count that fits nicely
                val stepPx = barWidthPx + barSpacingPx
                val maxBarsFitting = (totalWidth / stepPx).toInt().coerceAtLeast(8)

                // Resample original audio samples to match display width
                val displaySamples = resampleSamples(parsedSamples, maxBarsFitting)
                val barCount = displaySamples.size
                val actualStepPx = totalWidth / barCount.toFloat()
                val computedBarWidthPx = (actualStepPx * 0.7f).coerceIn(2f, barWidthPx)

                // Linear Gradient Brush for active (played) audio bars
                val activeBrush = Brush.horizontalGradient(
                    colors = listOf(activeColor, activeSecondaryColor),
                    startX = 0f,
                    endX = totalWidth * effectiveProgress.coerceAtLeast(0.01f)
                )

                // Draw each waveform bar
                for (i in 0 until barCount) {
                    val sample = displaySamples[i]
                    val barCenterFraction = (i + 0.5f) / barCount
                    val isPast = barCenterFraction <= effectiveProgress

                    // Dynamic wave animation when playing
                    val liveDynamics = if (isPlaying && isPast) {
                        val distanceToPlayhead = (effectiveProgress - barCenterFraction).coerceAtLeast(0f)
                        val travelingWave = sin(wavePhase + i * 0.35f) * 0.15f
                        val localPulse = if (distanceToPlayhead < 0.15f) (amplitudeBounce - 1f) * 0.5f else 0f
                        1.0f + travelingWave + localPulse
                    } else if (isPlaying) {
                        // Subtle idle breathing for upcoming bars
                        1.0f + sin(wavePhase * 0.5f + i * 0.2f) * 0.05f
                    } else {
                        1.0f
                    }

                    val normalizedHeight = (sample * liveDynamics).coerceIn(0.08f, 1.0f)
                    val barHeight = (maxHeight * normalizedHeight * 0.88f).coerceIn(minBarHeightPx, maxHeight)
                    val barX = i * actualStepPx + (actualStepPx - computedBarWidthPx) / 2f

                    when (style) {
                        WaveformStyle.SYMMETRICAL -> {
                            val barY = (maxHeight - barHeight) / 2f
                            if (isPast) {
                                drawRoundRect(
                                    brush = activeBrush,
                                    topLeft = Offset(barX, barY),
                                    size = Size(computedBarWidthPx, barHeight),
                                    cornerRadius = CornerRadius(computedBarWidthPx / 2f, computedBarWidthPx / 2f)
                                )
                            } else {
                                drawRoundRect(
                                    color = inactiveColor,
                                    topLeft = Offset(barX, barY),
                                    size = Size(computedBarWidthPx, barHeight),
                                    cornerRadius = CornerRadius(computedBarWidthPx / 2f, computedBarWidthPx / 2f)
                                )
                            }
                        }

                        WaveformStyle.BOTTOM_ALIGNED -> {
                            val barY = maxHeight - barHeight
                            if (isPast) {
                                drawRoundRect(
                                    brush = activeBrush,
                                    topLeft = Offset(barX, barY),
                                    size = Size(computedBarWidthPx, barHeight),
                                    cornerRadius = CornerRadius(computedBarWidthPx / 2f, computedBarWidthPx / 2f)
                                )
                            } else {
                                drawRoundRect(
                                    color = inactiveColor,
                                    topLeft = Offset(barX, barY),
                                    size = Size(computedBarWidthPx, barHeight),
                                    cornerRadius = CornerRadius(computedBarWidthPx / 2f, computedBarWidthPx / 2f)
                                )
                            }
                        }

                        WaveformStyle.REFLECTIVE -> {
                            // Top portion (70% height)
                            val topHeight = barHeight * 0.7f
                            val topY = (maxHeight * 0.65f) - topHeight
                            // Bottom reflection (30% height, lower opacity)
                            val bottomHeight = barHeight * 0.3f
                            val bottomY = maxHeight * 0.67f

                            val topColor = if (isPast) activeColor else inactiveColor
                            val botColor = (if (isPast) activeSecondaryColor else inactiveColor).copy(alpha = 0.4f)

                            drawRoundRect(
                                color = topColor,
                                topLeft = Offset(barX, topY),
                                size = Size(computedBarWidthPx, topHeight),
                                cornerRadius = CornerRadius(computedBarWidthPx / 2f, computedBarWidthPx / 2f)
                            )
                            drawRoundRect(
                                color = botColor,
                                topLeft = Offset(barX, bottomY),
                                size = Size(computedBarWidthPx, bottomHeight),
                                cornerRadius = CornerRadius(computedBarWidthPx / 2f, computedBarWidthPx / 2f)
                            )
                        }
                    }
                }

                // Draw Glowing Playhead Cursor Line & Head Pill
                if (showPlayhead) {
                    val playheadX = (totalWidth * effectiveProgress).coerceIn(0f, totalWidth)
                    val cursorColor = if (isDragging) activeSecondaryColor else playheadColor

                    // Playhead vertical needle line
                    drawLine(
                        color = cursorColor.copy(alpha = if (isPlaying || isDragging) 0.95f else 0.75f),
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, maxHeight),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Playhead glowing center knob
                    val knobRadius = if (isDragging) 5.dp.toPx() else 3.5.dp.toPx()
                    drawCircle(
                        color = cursorColor,
                        radius = knobRadius,
                        center = Offset(playheadX, maxHeight / 2f)
                    )

                    // Subtle halo glow when playing or dragging
                    if (isPlaying || isDragging) {
                        drawCircle(
                            color = cursorColor.copy(alpha = 0.25f * playheadGlow),
                            radius = knobRadius * 2.2f,
                            center = Offset(playheadX, maxHeight / 2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-level Interactive Audio Playback Card with waveform visualizer,
 * controls (play/pause, speed toggle, seek slider), and time elapsed badges.
 */
@Composable
fun InteractiveAudioWaveformCard(
    memoryId: String,
    attachmentId: String,
    waveformData: String,
    title: String,
    durationMs: Long,
    playbackState: AudioPlaybackState,
    modifier: Modifier = Modifier,
    transcription: String? = null,
    onPlayPause: () -> Unit = {
        AudioPlayerManager.playOrPause(memoryId, attachmentId, durationMs)
    },
    onSeek: (Float) -> Unit = { fraction ->
        AudioPlayerManager.seekTo(fraction)
    }
) {
    val isPlayingThis = playbackState.memoryId == memoryId &&
            playbackState.attachmentId == attachmentId &&
            playbackState.isPlaying

    val currentProgress = if (playbackState.memoryId == memoryId && playbackState.attachmentId == attachmentId) {
        playbackState.progress
    } else {
        0f
    }

    val currentMs = if (playbackState.memoryId == memoryId && playbackState.attachmentId == attachmentId) {
        playbackState.currentPositionMs
    } else {
        0L
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPlayingThis) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_audio_waveform_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title & Duration / Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.ifBlank { "Gravação de Memória" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Playback speed pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .testTag("speed_chip_${memoryId}")
                ) {
                    Text(
                        text = "${playbackState.playbackSpeed}x",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Visualizer & Play Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Play / Pause Button
                FilledIconButton(
                    onClick = onPlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPlayingThis) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_play_pause_${memoryId}")
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingThis) "Pausar" else "Reproduzir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Interactive Audio Waveform
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    PlaybackWaveformVisualizer(
                        waveformData = waveformData,
                        isPlaying = isPlayingThis,
                        progress = currentProgress,
                        totalDurationMs = durationMs,
                        activeColor = MaterialTheme.colorScheme.primary,
                        activeSecondaryColor = MaterialTheme.colorScheme.tertiary,
                        inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                        playheadColor = MaterialTheme.colorScheme.primary,
                        onSeek = onSeek,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timestamps: Elapsed & Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val elapsedSecs = (currentMs / 1000) % 60
                val elapsedMins = (currentMs / (1000 * 60)) % 60
                val totalSecs = (durationMs / 1000) % 60
                val totalMins = (durationMs / (1000 * 60)) % 60

                Text(
                    text = String.format("%02d:%02d", elapsedMins, elapsedSecs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = String.format("%02d:%02d", totalMins, totalSecs),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Optional Transcription snippet
            if (!transcription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“$transcription”",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Helper parsing & resampling algorithms
// -------------------------------------------------------------

/**
 * Parses raw waveform string into normalized float samples [0.0f..1.0f].
 * If empty or invalid, synthesizes an organic speech-like waveform pattern.
 */
private fun parseWaveformSamples(waveformData: String): List<Float> {
    if (waveformData.isBlank()) {
        return generateSyntheticVoiceProfile()
    }

    val parsed = waveformData.split(",")
        .mapNotNull { it.trim().toFloatOrNull() }

    if (parsed.isEmpty()) {
        return generateSyntheticVoiceProfile()
    }

    // Check if values are in 0-100 range and normalize to 0-1.0
    val maxVal = parsed.maxOrNull() ?: 1.0f
    return if (maxVal > 1.05f) {
        val safeMax = if (maxVal <= 0f) 100f else maxVal
        parsed.map { (it / safeMax).coerceIn(0.05f, 1.0f) }
    } else {
        parsed.map { it.coerceIn(0.05f, 1.0f) }
    }
}

/**
 * Synthesizes a natural, visually pleasing organic voice audio waveform pattern
 * with varied peaks, conversational pauses, and resonance harmonics.
 */
private fun generateSyntheticVoiceProfile(): List<Float> {
    return listOf(
        0.20f, 0.35f, 0.55f, 0.80f, 0.95f, 0.70f, 0.40f, 0.25f,
        0.50f, 0.75f, 0.90f, 0.85f, 0.60f, 0.30f, 0.15f, 0.35f,
        0.65f, 0.85f, 1.00f, 0.78f, 0.52f, 0.30f, 0.60f, 0.88f,
        0.72f, 0.45f, 0.22f, 0.40f, 0.70f, 0.92f, 0.68f, 0.35f,
        0.18f, 0.45f, 0.75f, 0.60f, 0.35f, 0.20f
    )
}

/**
 * Resamples input samples to target sample count using linear interpolation
 * to ensure crisp and uniform waveform bars across any screen size.
 */
private fun resampleSamples(input: List<Float>, targetCount: Int): List<Float> {
    if (input.isEmpty() || targetCount <= 0) return emptyList()
    if (input.size == targetCount) return input

    val result = ArrayList<Float>(targetCount)
    val ratio = (input.size - 1).toFloat() / (targetCount - 1).coerceAtLeast(1).toFloat()

    for (i in 0 until targetCount) {
        val sourceIndex = i * ratio
        val lowerIndex = sourceIndex.toInt().coerceIn(0, input.size - 1)
        val upperIndex = (lowerIndex + 1).coerceIn(0, input.size - 1)
        val fraction = sourceIndex - lowerIndex

        val interpolated = input[lowerIndex] * (1f - fraction) + input[upperIndex] * fraction
        result.add(interpolated.coerceIn(0.05f, 1.0f))
    }
    return result
}
