package com.example.priv.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Backwards-compatible audio waveform visualizer that leverages the enhanced
 * dynamic and interactive [PlaybackWaveformVisualizer] component engine.
 */
@Composable
fun AudioWaveformVisualizer(
    waveformData: String,
    isPlaying: Boolean,
    progress: Float, // 0.0f to 1.0f
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    barWidthDp: Float = 3.5f,
    barSpacingDp: Float = 2.5f,
    onSeek: (Float) -> Unit = {}
) {
    PlaybackWaveformVisualizer(
        waveformData = waveformData,
        isPlaying = isPlaying,
        progress = progress,
        modifier = modifier,
        activeColor = activeColor,
        activeSecondaryColor = MaterialTheme.colorScheme.tertiary,
        inactiveColor = inactiveColor,
        barWidthDp = barWidthDp,
        barSpacingDp = barSpacingDp,
        onSeek = onSeek
    )
}
