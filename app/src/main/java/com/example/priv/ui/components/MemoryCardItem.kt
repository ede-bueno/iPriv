package com.example.priv.ui.components

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.ui.audio.AudioPlayerManager

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryCardItem(
    memoryWithDetails: MemoryWithDetails,
    onToggleFavorite: () -> Unit,
    onMoveToTrash: () -> Unit,
    onPersonClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val memory = memoryWithDetails.memory
    val audioAttachment = memoryWithDetails.attachments.firstOrNull { it.type == "AUDIO" }
    val primaryPerson = memoryWithDetails.primaryPerson

    val playbackState by AudioPlayerManager.playbackState.collectAsState()

    val isThisAudioPlaying = audioAttachment != null &&
            playbackState.memoryId == memory.id &&
            playbackState.attachmentId == audioAttachment.id &&
            playbackState.isPlaying

    val currentProgress = if (audioAttachment != null && playbackState.memoryId == memory.id) {
        playbackState.progress
    } else 0f

    var showTranscription by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("memory_card_${memory.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Source + Person Chip + Date + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source badge (WhatsApp / Manual)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (memory.source == "WhatsApp") Color(0xFF25D366).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (memory.source == "WhatsApp") Icons.Default.ChatBubble else Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = if (memory.source == "WhatsApp") Color(0xFF25D366) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = memory.source,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (memory.source == "WhatsApp") Color(0xFF25D366) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Primary Person Chip
                    if (primaryPerson != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onPersonClick(primaryPerson.id) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(
                                            runCatching { Color(android.graphics.Color.parseColor(primaryPerson.colorHex)) }
                                                .getOrDefault(MaterialTheme.colorScheme.primary)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = primaryPerson.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (memory.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onMoveToTrash,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Mover para Lixeira",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = memory.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (memory.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.note,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Audio Player Component
            if (audioAttachment != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Play/Pause Button
                            FilledIconButton(
                                onClick = {
                                    AudioPlayerManager.playOrPause(
                                        memoryId = memory.id,
                                        attachmentId = audioAttachment.id,
                                        totalDurationMs = audioAttachment.durationMs
                                    )
                                },
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isThisAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isThisAudioPlaying) "Pausar" else "Tocar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Waveform
                            AudioWaveformVisualizer(
                                waveformData = audioAttachment.waveformData,
                                isPlaying = isThisAudioPlaying,
                                progress = currentProgress,
                                activeColor = MaterialTheme.colorScheme.primary,
                                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                onSeek = { fraction ->
                                    if (playbackState.memoryId == memory.id) {
                                        AudioPlayerManager.seekTo(fraction)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Speed Toggle (1x, 1.5x, 2x)
                            if (playbackState.memoryId == memory.id) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { AudioPlayerManager.toggleSpeed() }
                                ) {
                                    Text(
                                        text = "${playbackState.playbackSpeed}x",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Duration & Transcription toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val durationText = if (audioAttachment.durationMs > 0) {
                                val seconds = (audioAttachment.durationMs / 1000) % 60
                                val minutes = (audioAttachment.durationMs / (1000 * 60)) % 60
                                String.format("%d:%02d", minutes, seconds)
                            } else "0:24"

                            Text(
                                text = "Áudio • $durationText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (audioAttachment.transcription.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showTranscription = !showTranscription }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = "Transcrição",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showTranscription) "Ocultar texto" else "Ver texto",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showTranscription && audioAttachment.transcription.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "\"${audioAttachment.transcription}\"",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Tags + Relative Time in PT-BR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    memoryWithDetails.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "#${tag.name}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val diffMs = System.currentTimeMillis() - memory.timestamp
                val timeAgo = when {
                    diffMs < 60_000L -> "Agora mesmo"
                    diffMs < 3600_000L -> "${diffMs / 60_000L} min atrás"
                    diffMs < 86400_000L -> "${diffMs / 3600_000L} h atrás"
                    diffMs < 172800_000L -> "Ontem"
                    else -> "${diffMs / 86400_000L} dias atrás"
                }

                Text(
                    text = timeAgo,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
