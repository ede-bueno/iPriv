package com.example.priv.ui.audio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.data.entity.AudioMemoryEntity
import com.example.priv.data.repository.AudioMemoryRepository
import com.example.priv.ui.components.PlaybackWaveformVisualizer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMemoriesModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioRepo = remember { AudioMemoryRepository.getInstance(context) }

    val audioList by audioRepo.allAudioMemories.collectAsState(initial = emptyList())
    val totalCount by audioRepo.totalAudioCount.collectAsState(initial = 0)
    val totalDurationMs by audioRepo.totalDurationMs.collectAsState(initial = 0L)
    val totalSizeBytes by audioRepo.totalSizeBytes.collectAsState(initial = 0L)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, FAVORITES, WITH_TRANSCRIPTION, MIC, TAG_FAMILIA, TAG_VIAGEM, TAG_TRABALHO
    var showRecorderModal by remember { mutableStateOf(false) }

    // Dialog state for editing title, tags and transcription
    var editingAudio by remember { mutableStateOf<AudioMemoryEntity?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editTags by remember { mutableStateOf("") }
    var editTranscriptionText by remember { mutableStateOf("") }

    // Dialog state for confirming deletion
    var deletingAudio by remember { mutableStateOf<AudioMemoryEntity?>(null) }

    // Busca reativa com Room quando há texto de pesquisa
    val roomSearchResults by remember(searchQuery) {
        if (searchQuery.isNotBlank()) {
            audioRepo.searchByTitleOrTranscription(searchQuery)
        } else {
            audioRepo.allAudioMemories
        }
    }.collectAsState(initial = emptyList())

    val baseList = if (searchQuery.isNotBlank()) roomSearchResults else audioList

    val filteredList = remember(baseList, searchQuery, selectedFilter) {
        baseList.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "FAVORITES" -> item.isFavorite
                "WITH_TRANSCRIPTION" -> item.transcriptionText.isNotBlank()
                "MIC" -> item.source == "MICROPHONE"
                "TAG_FAMILIA" -> item.tags.contains("família", ignoreCase = true) || item.tags.contains("familia", ignoreCase = true)
                "TAG_VIAGEM" -> item.tags.contains("viagem", ignoreCase = true)
                "TAG_TRABALHO" -> item.tags.contains("trabalho", ignoreCase = true)
                else -> true
            }

            matchesFilter
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        modifier = modifier.fillMaxHeight(0.92f).testTag("sheet_audio_memories_vault")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Memórias de Áudio & Transcrições",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Banco de dados local (Room) persistente",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(36.dp).testTag("btn_close_audio_vault")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioStatCard(
                    title = "Áudios",
                    value = totalCount.toString(),
                    icon = Icons.Default.Mic,
                    modifier = Modifier.weight(1f)
                )
                AudioStatCard(
                    title = "Tempo Total",
                    value = formatTotalTime(totalDurationMs),
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1.2f)
                )
                AudioStatCard(
                    title = "Armazenamento",
                    value = formatTotalBytes(totalSizeBytes),
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1.2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Record New Audio
            Button(
                onClick = { showRecorderModal = true },
                modifier = Modifier.fillMaxWidth().testTag("btn_record_new_audio_vault"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gravar Novo Áudio com Microfone 🎙️", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("input_search_audio_vault"),
                placeholder = { Text("Buscar em transcrições, títulos ou tags...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips com suporte a tags (família, viagem, trabalho)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Todos (${audioList.size})", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "TAG_FAMILIA",
                        onClick = { selectedFilter = if (selectedFilter == "TAG_FAMILIA") "ALL" else "TAG_FAMILIA" },
                        label = { Text("Família 👨‍👩‍👧", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "TAG_VIAGEM",
                        onClick = { selectedFilter = if (selectedFilter == "TAG_VIAGEM") "ALL" else "TAG_VIAGEM" },
                        label = { Text("Viagem ✈️", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "TAG_TRABALHO",
                        onClick = { selectedFilter = if (selectedFilter == "TAG_TRABALHO") "ALL" else "TAG_TRABALHO" },
                        label = { Text("Trabalho 💼", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "FAVORITES",
                        onClick = { selectedFilter = "FAVORITES" },
                        label = { Text("Favoritos ⭐", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "WITH_TRANSCRIPTION",
                        onClick = { selectedFilter = "WITH_TRANSCRIPTION" },
                        label = { Text("Com Transcrição 📝", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "MIC",
                        onClick = { selectedFilter = "MIC" },
                        label = { Text("Voz 🎙️", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Audio List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HearingDisabled,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Nenhum áudio encontrado para \"$searchQuery\"" else "Nenhuma memória de áudio salva",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Grave novos áudios ou importe mensagens para salvá-los localmente no Room.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredList, key = { it.id }) { audioItem ->
                        AudioMemoryCard(
                            audio = audioItem,
                            onPlayPause = {
                                AudioPlayerManager.playOrPause(
                                    memoryId = audioItem.id,
                                    attachmentId = audioItem.id,
                                    totalDurationMs = audioItem.durationMs,
                                    waveformData = audioItem.waveformData
                                )
                            },
                            onToggleFavorite = {
                                coroutineScope.launch {
                                    audioRepo.toggleFavorite(audioItem.id, !audioItem.isFavorite)
                                }
                            },
                            onEditTranscription = {
                                editingAudio = audioItem
                                editTitle = audioItem.title
                                editTags = audioItem.tags
                                editTranscriptionText = audioItem.transcriptionText
                            },
                            onCopyTranscription = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Transcrição", audioItem.transcriptionText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Transcrição copiada para a área de transferência! 📋", Toast.LENGTH_SHORT).show()
                            },
                            onRetranscribe = {
                                coroutineScope.launch {
                                    Toast.makeText(context, "Transcrevendo com IA...", Toast.LENGTH_SHORT).show()
                                    val newText = audioRepo.transcribeWithAI(audioItem.id, audioItem.title)
                                    Toast.makeText(context, "Transcrição concluída com sucesso! ⚡", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                deletingAudio = audioItem
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal para Gravação com Microfone
    if (showRecorderModal) {
        AudioRecordingBottomSheet(
            onDismissRequest = { showRecorderModal = false },
            onAudioRecorded = { recordedAudio ->
                Toast.makeText(context, "Áudio salvo no banco de dados Room: ${recordedAudio.name} 🎙️", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Dialog para Edição de Metadados e Transcrição
    if (editingAudio != null) {
        AlertDialog(
            onDismissRequest = { editingAudio = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Editar Memória de Áudio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Edição de Título
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Título da Gravação") },
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_audio_title"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Tags rápidas e campo de texto
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Tags da Memória (ex: família, viagem, trabalho):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("família", "viagem", "trabalho").forEach { tagCandidate ->
                                val currentTagList = editTags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                val isSelected = tagCandidate.lowercase() in currentTagList
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val mutable = currentTagList.toMutableList()
                                        if (isSelected) {
                                            mutable.remove(tagCandidate.lowercase())
                                        } else {
                                            mutable.add(tagCandidate.lowercase())
                                        }
                                        editTags = mutable.joinToString(", ")
                                    },
                                    label = { Text("#$tagCandidate", fontSize = 11.sp) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = editTags,
                            onValueChange = { editTags = it },
                            label = { Text("Tags personalizadas (separadas por vírgula)") },
                            placeholder = { Text("família, viagem, trabalho, reunião") },
                            modifier = Modifier.fillMaxWidth().testTag("input_edit_audio_tags"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Edição de Transcrição
                    OutlinedTextField(
                        value = editTranscriptionText,
                        onValueChange = { editTranscriptionText = it },
                        label = { Text("Conteúdo da Transcrição") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp).testTag("input_edit_transcription"),
                        placeholder = { Text("Digite ou corrija a transcrição do áudio...") },
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = editingAudio
                        if (target != null) {
                            coroutineScope.launch {
                                audioRepo.updateAudioMetadata(
                                    id = target.id,
                                    title = editTitle.ifBlank { target.title },
                                    tags = editTags,
                                    transcriptionText = editTranscriptionText
                                )
                                Toast.makeText(context, "Memória de áudio e tags atualizadas no Room! 💾", Toast.LENGTH_SHORT).show()
                            }
                        }
                        editingAudio = null
                    },
                    modifier = Modifier.testTag("btn_save_audio_metadata_edit")
                ) {
                    Text("Salvar no Banco")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAudio = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Dialog para Confirmação de Exclusão
    if (deletingAudio != null) {
        AlertDialog(
            onDismissRequest = { deletingAudio = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Excluir Memória de Áudio?") },
            text = {
                Text("Esta ação removerá os metadados e a transcrição associada do banco de dados local Room.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = deletingAudio
                        if (target != null) {
                            coroutineScope.launch {
                                audioRepo.deleteAudio(target.id)
                                Toast.makeText(context, "Áudio removido do Room.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        deletingAudio = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAudio = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AudioStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AudioMemoryCard(
    audio: AudioMemoryEntity,
    onPlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEditTranscription: () -> Unit,
    onCopyTranscription: () -> Unit,
    onRetranscribe: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by AudioPlayerManager.playbackState.collectAsState()
    val isPlayingThis = playbackState.memoryId == audio.id && playbackState.isPlaying

    val formattedDate = remember(audio.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(audio.createdAt))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when (audio.source) {
                            "MICROPHONE" -> Color(0xFFE8F5E9)
                            "WHATSAPP" -> Color(0xFFE0F2F1)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (audio.source) {
                                    "MICROPHONE" -> Icons.Default.Mic
                                    "WHATSAPP" -> Icons.Default.ChatBubble
                                    else -> Icons.Default.AudioFile
                                },
                                contentDescription = null,
                                tint = when (audio.source) {
                                    "MICROPHONE" -> Color(0xFF2E7D32)
                                    "WHATSAPP" -> Color(0xFF00796B)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = audio.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${audio.speakerName} • $formattedDate",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (audio.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favoritar",
                            tint = if (audio.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Technical Metadata Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetadataBadge(icon = Icons.Default.Timer, text = audio.formattedDuration)
                MetadataBadge(icon = Icons.Default.DataUsage, text = audio.formattedSize)
                MetadataBadge(icon = Icons.Default.Tune, text = "${audio.sampleRate / 1000}kHz")
                MetadataBadge(icon = Icons.Default.Security, text = "SHA-256")
            }

            // Tags Badges (ex: família, viagem, trabalho)
            if (audio.tags.isNotBlank()) {
                val tagsList = audio.tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
                if (tagsList.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagsList.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (tag.lowercase()) {
                                    "família", "familia" -> Color(0xFFE8F5E9)
                                    "viagem" -> Color(0xFFE3F2FD)
                                    "trabalho" -> Color(0xFFFFF3E0)
                                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                }
                            ) {
                                Text(
                                    text = "#$tag",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (tag.lowercase()) {
                                        "família", "familia" -> Color(0xFF2E7D32)
                                        "viagem" -> Color(0xFF1565C0)
                                        "trabalho" -> Color(0xFFE65100)
                                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Inline Audio Player Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingThis) "Pausar" else "Reproduzir",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        PlaybackWaveformVisualizer(
                            waveformData = audio.waveformData,
                            isPlaying = isPlayingThis,
                            progress = if (isPlayingThis) playbackState.progress else 0f,
                            totalDurationMs = audio.durationMs,
                            activeColor = MaterialTheme.colorScheme.primary,
                            activeSecondaryColor = MaterialTheme.colorScheme.tertiary,
                            inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            playheadColor = MaterialTheme.colorScheme.primary,
                            onSeek = { fraction ->
                                if (isPlayingThis) {
                                    AudioPlayerManager.seekTo(fraction)
                                } else {
                                    onPlayPause()
                                    AudioPlayerManager.seekTo(fraction)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isPlayingThis) formatMillis(playbackState.currentPositionMs) else "00:00",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = audio.formattedDuration,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Associated Text Transcription Box
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Transcrição de Texto Associada",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (audio.transcriptionStatus) {
                                "COMPLETED" -> Color(0xFFE8F5E9)
                                "EDITED" -> Color(0xFFE1F5FE)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = when (audio.transcriptionStatus) {
                                    "COMPLETED" -> "Transcrito (${(audio.transcriptionConfidence * 100).toInt()}%)"
                                    "EDITED" -> "Editado pelo Usuário"
                                    else -> audio.transcriptionStatus
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (audio.transcriptionStatus) {
                                    "COMPLETED" -> Color(0xFF2E7D32)
                                    "EDITED" -> Color(0xFF0277BD)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = if (audio.transcriptionText.isNotBlank()) audio.transcriptionText else "(Sem transcrição no momento)",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Action buttons for transcription
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onCopyTranscription,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copiar", fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        TextButton(
                            onClick = onRetranscribe,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("IA ⚡", fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalButton(
                            onClick = onEditTranscription,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatTotalTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}m ${seconds}s"
}

private fun formatTotalBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}

private fun formatMillis(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
