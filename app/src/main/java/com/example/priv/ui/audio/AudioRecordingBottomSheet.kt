package com.example.priv.ui.audio

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.data.preferences.AudioRecordingQuality
import com.example.priv.data.preferences.UserPreferencesManager
import com.example.priv.util.audio.AudioRecorderManager
import com.example.priv.util.audio.AudioRecordingState
import com.example.priv.util.audio.LocalAudioFile

/**
 * BottomSheet interativo para gravação de áudio de alta qualidade com microfone,
 * visualização de ondas em tempo real, cronômetro e salvamento em arquivo local.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRecordingBottomSheet(
    onDismissRequest: () -> Unit,
    onAudioRecorded: (LocalAudioFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recorderManager = remember { AudioRecorderManager(context) }
    val audioRepo = remember { com.example.priv.data.repository.AudioMemoryRepository.getInstance(context) }
    val prefsManager = remember { UserPreferencesManager.getInstance(context) }
    val preferences by prefsManager.preferences.collectAsState()

    val recordingState by recorderManager.recordingState.collectAsState()
    var hasPermission by remember { mutableStateOf(recorderManager.hasRecordPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            recorderManager.startRecording(
                highQuality = preferences.audioQuality == AudioRecordingQuality.HIGH_AAC_128K
            )
        } else {
            Toast.makeText(context, "Permissão de microfone necessária para gravar áudios.", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recorderManager.recordingState.value is AudioRecordingState.Recording) {
                recorderManager.cancelRecording()
            }
        }
    }

    // Pulse animation for recording indicator
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ModalBottomSheet(
        onDismissRequest = {
            recorderManager.cancelRecording()
            onDismissRequest()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        modifier = modifier.testTag("audio_recording_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Gravação de Voz do GuardDay",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            when (val state = recordingState) {
                is AudioRecordingState.Idle -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Text(
                            text = "Toque abaixo para iniciar a gravação",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (hasPermission) {
                                    recorderManager.startRecording(
                                        highQuality = preferences.audioQuality == AudioRecordingQuality.HIGH_AAC_128K
                                    )
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_start_recording")
                        ) {
                            Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Iniciar Gravação de Áudio", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is AudioRecordingState.Recording -> {
                    val durationSeconds = (state.elapsedDurationMs / 1000).toInt()
                    val minutes = durationSeconds / 60
                    val seconds = durationSeconds % 60
                    val formattedTime = "%02d:%02d".format(minutes, seconds)

                    // Big Pulsing Recording Mic
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        if (!state.isPaused) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (state.isPaused) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPaused) Icons.Default.Pause else Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (state.isPaused) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Recording timer
                    Text(
                        text = formattedTime,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (state.isPaused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (state.isPaused) "Gravação pausada" else "Gravando com microfone...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Dynamic Waveform Visualizer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val amplitudes = state.waveformHistory.takeLast(28)
                        amplitudes.forEach { amp ->
                            val barHeight = (amp * 40.dp.value).coerceIn(4f, 40f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Controls Row: Cancel, Pause/Resume, Stop & Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel / Discard
                        IconButton(
                            onClick = {
                                recorderManager.cancelRecording()
                                onDismissRequest()
                            },
                            modifier = Modifier.size(52.dp).testTag("btn_cancel_recording")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Descartar", tint = MaterialTheme.colorScheme.error)
                        }

                        // Pause / Resume
                        FilledTonalIconButton(
                            onClick = {
                                if (state.isPaused) {
                                    recorderManager.resumeRecording()
                                } else {
                                    recorderManager.pauseRecording()
                                }
                            },
                            modifier = Modifier.size(56.dp).testTag("btn_pause_resume_recording")
                        ) {
                            Icon(
                                imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (state.isPaused) "Retomar" else "Pausar"
                            )
                        }

                        // Stop & Save
                        Button(
                            onClick = {
                                val result = recorderManager.stopRecording()
                                if (result != null) {
                                    coroutineScope.launch {
                                        try {
                                            audioRepo.saveFromRecordedFile(result)
                                        } catch (e: Exception) {
                                            // Handled gracefully
                                        }
                                    }
                                    onAudioRecorded(result)
                                    onDismissRequest()
                                } else {
                                    Toast.makeText(context, "Gravação muito curta ou inválida", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = CircleShape,
                            modifier = Modifier.height(52.dp).testTag("btn_save_recording")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is AudioRecordingState.Completed -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(48.dp))
                        Text("Áudio Gravado com Sucesso!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(state.file.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Button(
                            onClick = {
                                recorderManager.resetState()
                                onDismissRequest()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Concluído")
                        }
                    }
                }

                is AudioRecordingState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Text("Erro na gravação: ${state.message}", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        Button(
                            onClick = { recorderManager.resetState() }
                        ) {
                            Text("Tentar Novamente")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
