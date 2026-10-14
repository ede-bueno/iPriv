package com.example.priv.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.data.entity.PersonEntity
import com.example.priv.ui.audio.AudioPlaybackState
import com.example.priv.ui.audio.AudioPlayerManager

@Composable
fun MiniAudioPlayerBar(
    playbackState: AudioPlaybackState,
    title: String,
    person: PersonEntity?,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onExpandPlayer() }
            .testTag("mini_audio_player_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Linear progress indicator on top of mini bar
            LinearProgressIndicator(
                progress = { playbackState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Person Avatar or Audio Icon
                    if (person != null) {
                        Surface(
                            shape = CircleShape,
                            color = runCatching { Color(android.graphics.Color.parseColor(person.colorHex)) }
                                .getOrDefault(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = person.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title.ifBlank { "Tocando memória de áudio" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val remainingMs = (playbackState.totalDurationMs * (1f - playbackState.progress)).toLong().coerceAtLeast(0L)
                        val remSecs = (remainingMs / 1000) % 60
                        val remMins = (remainingMs / (1000 * 60)) % 60

                        Text(
                            text = if (person != null) "${person.name} • restam ${String.format("%d:%02d", remMins, remSecs)}"
                            else "restam ${String.format("%d:%02d", remMins, remSecs)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Play/Pause button
                    IconButton(
                        onClick = {
                            if (playbackState.memoryId != null && playbackState.attachmentId != null) {
                                AudioPlayerManager.playOrPause(
                                    playbackState.memoryId,
                                    playbackState.attachmentId,
                                    playbackState.totalDurationMs
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pausar" else "Tocar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Speed Toggle Pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { AudioPlayerManager.toggleSpeed() }
                    ) {
                        Text(
                            text = "${playbackState.playbackSpeed}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedAudioPlayerSheet(
    playbackState: AudioPlaybackState,
    title: String,
    person: PersonEntity?,
    waveformData: String = "",
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("expanded_audio_player_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REPRODUTOR PREMIUM PRIV",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimizar", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Hero Avatar/Waveform Box
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (person != null) {
                        Surface(
                            shape = CircleShape,
                            color = runCatching { Color(android.graphics.Color.parseColor(person.colorHex)) }
                                .getOrDefault(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = person.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 26.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = person.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = title.ifBlank { "Áudio da Memória" },
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Waveform Visualizer Large
            PlaybackWaveformVisualizer(
                waveformData = waveformData.ifBlank { "0.3,0.7,0.9,0.5,0.8,0.4,0.9,0.2,0.6,0.8,0.4,0.7,0.5,0.8,0.9,0.3,0.6,0.8" },
                isPlaying = playbackState.isPlaying,
                progress = playbackState.progress,
                totalDurationMs = playbackState.totalDurationMs,
                activeColor = MaterialTheme.colorScheme.primary,
                activeSecondaryColor = MaterialTheme.colorScheme.tertiary,
                inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                playheadColor = MaterialTheme.colorScheme.primary,
                showPlayhead = true,
                showScrubTooltip = true,
                barWidthDp = 4f,
                barSpacingDp = 3f,
                onSeek = { AudioPlayerManager.seekTo(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Seek Slider
            Slider(
                value = playbackState.progress,
                onValueChange = { AudioPlayerManager.seekTo(it) },
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Current Time & Remaining Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currMs = playbackState.currentPositionMs
                val currSecs = (currMs / 1000) % 60
                val currMins = (currMs / (1000 * 60)) % 60

                val totalMs = playbackState.totalDurationMs
                val totalSecs = (totalMs / 1000) % 60
                val totalMins = (totalMs / (1000 * 60)) % 60

                Text(
                    text = String.format("%d:%02d", currMins, currSecs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = String.format("-%d:%02d", (totalMs - currMs) / (1000 * 60), ((totalMs - currMs) / 1000) % 60),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s
                IconButton(
                    onClick = {
                        val newProgress = (playbackState.progress - 0.1f).coerceAtLeast(0f)
                        AudioPlayerManager.seekTo(newProgress)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "Voltar 10s", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                }

                // Play / Pause Main Button
                FilledIconButton(
                    onClick = {
                        if (playbackState.memoryId != null && playbackState.attachmentId != null) {
                            AudioPlayerManager.playOrPause(
                                playbackState.memoryId,
                                playbackState.attachmentId,
                                playbackState.totalDurationMs
                            )
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pausar" else "Tocar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Forward 10s
                IconButton(
                    onClick = {
                        val newProgress = (playbackState.progress + 0.1f).coerceAtMost(1f)
                        AudioPlayerManager.seekTo(newProgress)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "Avançar 10s", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed Switcher Pill Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    val isSel = playbackState.playbackSpeed == speed
                    FilterChip(
                        selected = isSel,
                        onClick = { AudioPlayerManager.setSpeed(speed) },
                        label = { Text("${speed}x", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
