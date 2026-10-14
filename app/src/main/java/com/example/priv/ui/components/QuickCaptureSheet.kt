package com.example.priv.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.priv.data.entity.CollectionEntity
import com.example.priv.data.entity.GroupEntity
import com.example.priv.data.entity.MomentEntity
import com.example.priv.ui.audio.AudioRecordingBottomSheet
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.model.AttachmentInput
import com.example.priv.data.model.MemoryWithDetails
import kotlinx.coroutines.launch

enum class CaptureFlowMode {
    NEW_MEMORY,
    APPEND_TO_EXISTING
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickCaptureSheet(
    persons: List<PersonEntity>,
    groups: List<GroupEntity> = emptyList(),
    moments: List<MomentEntity> = emptyList(),
    collections: List<CollectionEntity> = emptyList(),
    existingMemories: List<MemoryWithDetails> = emptyList(),
    initialAttachments: List<AttachmentInput> = emptyList(),
    onDismiss: () -> Unit,
    onCreatePersonInline: (name: String, rel: String, onCreated: (String) -> Unit) -> Unit = { _, _, _ -> },
    onCreateGroupInline: (name: String, desc: String, onCreated: (String) -> Unit) -> Unit = { _, _, _ -> },
    onCreateMomentInline: (title: String, desc: String, date: String, onCreated: (String) -> Unit) -> Unit = { _, _, _, _ -> },
    onCreateCollectionInline: (title: String, desc: String, onCreated: (String) -> Unit) -> Unit = { _, _, _ -> },
    onSaveNew: (
        title: String,
        note: String,
        attachments: List<AttachmentInput>,
        personIds: List<String>,
        groupIds: List<String>,
        momentIds: List<String>,
        collectionIds: List<String>,
        tags: String
    ) -> Unit,
    onAppendToExisting: (
        targetMemoryId: String,
        attachments: List<AttachmentInput>,
        additionalNote: String
    ) -> Unit = { _, _, _ -> }
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var flowMode by rememberSaveable { mutableStateOf(CaptureFlowMode.NEW_MEMORY) }
    var selectedExistingMemoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var existingSearchQuery by rememberSaveable { mutableStateOf("") }

    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }

    val selectedPersonIds = remember { mutableStateListOf<String>() }
    val selectedGroupIds = remember { mutableStateListOf<String>() }
    val selectedMomentIds = remember { mutableStateListOf<String>() }
    val selectedCollectionIds = remember { mutableStateListOf<String>() }

    var isSaving by remember { mutableStateOf(false) }
    var isAiGenerating by remember { mutableStateOf(false) }
    var showDiscardConfirmation by remember { mutableStateOf(false) }

    // Dialog state for inline creation
    var inlineCreateType by remember { mutableStateOf<String?>(null) }
    var inlineName by remember { mutableStateOf("") }
    var inlineDesc by remember { mutableStateOf("") }
    var showImportContactsInSheet by remember { mutableStateOf(false) }

    val attachments = remember {
        mutableStateListOf<AttachmentInput>().apply {
            if (initialAttachments.isNotEmpty()) {
                addAll(initialAttachments)
            }
        }
    }

    val hasUnsavedChanges = remember(title, note, tags, attachments.size, selectedExistingMemoryId) {
        title.isNotBlank() || note.isNotBlank() || tags.isNotBlank() || attachments.isNotEmpty() || selectedExistingMemoryId != null
    }

    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isImeVisible || hasUnsavedChanges) {
        if (isImeVisible) {
            focusManager.clearFocus()
        } else if (hasUnsavedChanges && !isSaving) {
            showDiscardConfirmation = true
        } else {
            onDismiss()
        }
    }

    fun safeDismiss() {
        if (hasUnsavedChanges && !isSaving) {
            showDiscardConfirmation = true
        } else {
            onDismiss()
        }
    }

    fun performSave() {
        if (isSaving) return
        isSaving = true
        focusManager.clearFocus()

        if (flowMode == CaptureFlowMode.APPEND_TO_EXISTING && selectedExistingMemoryId != null) {
            onAppendToExisting(
                selectedExistingMemoryId!!,
                attachments.toList(),
                note
            )
        } else {
            onSaveNew(
                title.ifBlank { "Nova memória GuardDay" },
                note,
                attachments.toList(),
                selectedPersonIds.toList(),
                selectedGroupIds.toList(),
                selectedMomentIds.toList(),
                selectedCollectionIds.toList(),
                tags
            )
        }
    }

    // Media Launchers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val meta = com.example.priv.data.util.MediaMetadataExtractor.extractMetadata(context, uri)
            attachments.add(
                AttachmentInput(
                    type = "IMAGE",
                    uri = uri.toString(),
                    mimeType = meta.mimeType ?: "image/jpeg",
                    fileName = meta.fileName ?: "",
                    fileSize = meta.fileSize ?: 0L,
                    width = meta.width,
                    height = meta.height,
                    capturedTimestamp = meta.capturedTimestamp
                )
            )
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            attachments.add(
                AttachmentInput(
                    type = "VIDEO",
                    uri = it.toString(),
                    mimeType = "video/mp4"
                )
            )
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            attachments.add(
                AttachmentInput(
                    type = "AUDIO",
                    uri = it.toString(),
                    durationMs = 45000L,
                    waveformData = "0.3,0.8,0.5,0.9,0.4,0.7,0.6",
                    mimeType = "audio/ogg"
                )
            )
        }
    }

    var showAudioCaptureOptions by remember { mutableStateOf(false) }
    var showAudioRecorderModal by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = { safeDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        ) {
            // Futuristic Header with Neural Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (flowMode == CaptureFlowMode.NEW_MEMORY) "Guardar Nova Memória" else "Vincular a Memória Existente",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preservação inteligente GuardDay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { performSave() },
                        enabled = !isSaving && (flowMode == CaptureFlowMode.NEW_MEMORY || selectedExistingMemoryId != null),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("top_save_quick_memory_btn")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (flowMode == CaptureFlowMode.NEW_MEMORY) "Guardar" else "Vincular",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { safeDismiss() },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
            }

            // Mode Selector Pill (Nova Memória vs Vincular a Existente)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (flowMode == CaptureFlowMode.NEW_MEMORY) MaterialTheme.colorScheme.surface else Color.Transparent,
                        tonalElevation = if (flowMode == CaptureFlowMode.NEW_MEMORY) 3.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { flowMode = CaptureFlowMode.NEW_MEMORY }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = if (flowMode == CaptureFlowMode.NEW_MEMORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Criar Nova",
                                fontSize = 12.sp,
                                fontWeight = if (flowMode == CaptureFlowMode.NEW_MEMORY) FontWeight.Bold else FontWeight.Medium,
                                color = if (flowMode == CaptureFlowMode.NEW_MEMORY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (flowMode == CaptureFlowMode.APPEND_TO_EXISTING) MaterialTheme.colorScheme.surface else Color.Transparent,
                        tonalElevation = if (flowMode == CaptureFlowMode.APPEND_TO_EXISTING) 3.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { flowMode = CaptureFlowMode.APPEND_TO_EXISTING }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = if (flowMode == CaptureFlowMode.APPEND_TO_EXISTING) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Vincular a Existente",
                                fontSize = 12.sp,
                                fontWeight = if (flowMode == CaptureFlowMode.APPEND_TO_EXISTING) FontWeight.Bold else FontWeight.Medium,
                                color = if (flowMode == CaptureFlowMode.APPEND_TO_EXISTING) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Attached Media Grid
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mídias & Arquivos (${attachments.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Áudio, Fotos e Vídeos",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Add Buttons Row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { photoPickerLauncher.launch("image/*") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Foto", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { videoPickerLauncher.launch("video/*") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Vídeo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAudioCaptureOptions = true }
                                    .testTag("btn_capture_audio")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Áudio", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }

                        if (attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(attachments.toList()) { att ->
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                                    ) {
                                        when (att.type) {
                                            "IMAGE" -> {
                                                AsyncImage(
                                                    model = att.uri,
                                                    contentDescription = "Foto",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "VIDEO" -> {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                                ) {
                                                    Icon(Icons.Default.PlayCircle, contentDescription = "Vídeo", tint = MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                            "AUDIO" -> {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Icon(Icons.Default.GraphicEq, contentDescription = "Áudio", tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            else -> {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                ) {
                                                    Icon(Icons.Default.Notes, contentDescription = "Texto", tint = MaterialTheme.colorScheme.tertiary)
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { attachments.remove(att) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(22.dp)
                                                .padding(2.dp)
                                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // If APPEND_TO_EXISTING: show search list of active memories
                if (flowMode == CaptureFlowMode.APPEND_TO_EXISTING) {
                    item {
                        Column {
                            Text(
                                text = "Escolha a Memória para Vincular",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = existingSearchQuery,
                                onValueChange = { existingSearchQuery = it },
                                placeholder = { Text("Buscar memória por título ou pessoa...", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val filteredExisting = remember(existingMemories, existingSearchQuery) {
                                if (existingSearchQuery.isBlank()) existingMemories.take(15)
                                else existingMemories.filter {
                                    it.memory.title.contains(existingSearchQuery, ignoreCase = true) ||
                                    it.primaryPerson?.name?.contains(existingSearchQuery, ignoreCase = true) == true
                                }
                            }

                            if (filteredExisting.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Nenhuma memória encontrada. Mude a busca ou crie uma nova memória.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredExisting.forEach { memWithDetails ->
                                        val isSelected = selectedExistingMemoryId == memWithDetails.memory.id
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { selectedExistingMemoryId = memWithDetails.memory.id }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = memWithDetails.memory.title,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (memWithDetails.primaryPerson != null) {
                                                        Text(
                                                            text = "Pessoa: ${memWithDetails.primaryPerson.name}",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Text(
                                                        text = "${memWithDetails.attachments.size} arquivos já salvos",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedExistingMemoryId = memWithDetails.memory.id }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("Nota adicional sobre os novos arquivos") },
                                placeholder = { Text("Ex: Fotos enviadas logo depois do áudio...") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                maxLines = 3
                            )
                        }
                    }
                } else {
                    // NEW_MEMORY Flow: Form with AI Assist
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "História & Contexto",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TextButton(
                                onClick = {
                                    if (!isAiGenerating) {
                                        isAiGenerating = true
                                        scope.launch {
                                            val selectedPerson = persons.firstOrNull { selectedPersonIds.contains(it.id) }
                                            val mgr = com.example.priv.data.ai.PrivGeminiIntelligenceManager(context)
                                            val result = mgr.analyzeAndSuggest(
                                                noteText = note,
                                                personName = selectedPerson?.name,
                                                audioDurationMs = attachments.firstOrNull { it.type == "AUDIO" }?.durationMs ?: 30000L
                                            )
                                            title = result.title
                                            if (note.isBlank()) {
                                                note = result.summaryOrTranscription
                                            }
                                            tags = result.tags.joinToString(", ")
                                            isAiGenerating = false
                                        }
                                    }
                                },
                                enabled = !isAiGenerating,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                if (isAiGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analisando...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("✨ Sugerir com IA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Título da Memória") },
                            placeholder = { Text("Ex: Conselhos de vida do avô") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("capture_title_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Notas, contexto e história") },
                            placeholder = { Text("Por que essa mensagem é importante? O que você sentiu?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("capture_note_input"),
                            shape = RoundedCornerShape(16.dp),
                            minLines = 3,
                            maxLines = 5
                        )
                    }

                    // People Association
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pessoas Envolvidas",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showImportContactsInSheet = true },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Agenda", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(
                                    onClick = {
                                        inlineName = ""
                                        inlineDesc = ""
                                        inlineCreateType = "PERSON"
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("+ Nova", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(persons) { person ->
                                val isSelected = selectedPersonIds.contains(person.id)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedPersonIds.remove(person.id)
                                        else selectedPersonIds.add(person.id)
                                    },
                                    label = { Text(person.name, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    runCatching { Color(android.graphics.Color.parseColor(person.colorHex)) }
                                                        .getOrDefault(MaterialTheme.colorScheme.primary)
                                                )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Moments & Collections Association
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Momentos & Eventos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            TextButton(
                                onClick = {
                                    inlineName = ""
                                    inlineDesc = ""
                                    inlineCreateType = "MOMENT"
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Momento", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(moments) { moment ->
                                val isSelected = selectedMomentIds.contains(moment.id)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedMomentIds.remove(moment.id)
                                        else selectedMomentIds.add(moment.id)
                                    },
                                    label = { Text(moment.title, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Tags
                    item {
                        OutlinedTextField(
                            value = tags,
                            onValueChange = { tags = it },
                            label = { Text("Tags / Palavras-chave") },
                            placeholder = { Text("Ex: conselho, aniversário, risadas") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Discard Confirmation Dialog
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Descartar alterações?", fontWeight = FontWeight.Bold) },
            text = { Text("Você tem conteúdos ou arquivos selecionados. Deseja continuar editando ou fechar?", fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { showDiscardConfirmation = false }) {
                    Text("Continuar Editando")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        onDismiss()
                    }
                ) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Inline Creation Dialog
    if (inlineCreateType != null) {
        AlertDialog(
            onDismissRequest = { inlineCreateType = null },
            title = {
                Text(
                    text = when (inlineCreateType) {
                        "PERSON" -> "Nova Pessoa"
                        "GROUP" -> "Novo Grupo"
                        "MOMENT" -> "Novo Momento"
                        else -> "Nova Coleção"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inlineName,
                        onValueChange = { inlineName = it },
                        label = { Text("Nome / Título") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inlineDesc,
                        onValueChange = { inlineDesc = it },
                        label = { Text("Descrição / Relação") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inlineName.isNotBlank()) {
                            when (inlineCreateType) {
                                "PERSON" -> onCreatePersonInline(inlineName, inlineDesc) { id -> selectedPersonIds.add(id) }
                                "GROUP" -> onCreateGroupInline(inlineName, inlineDesc) { id -> selectedGroupIds.add(id) }
                                "MOMENT" -> onCreateMomentInline(inlineName, inlineDesc, "2026") { id -> selectedMomentIds.add(id) }
                                "COLLECTION" -> onCreateCollectionInline(inlineName, inlineDesc) { id -> selectedCollectionIds.add(id) }
                            }
                            inlineCreateType = null
                        }
                    },
                    enabled = inlineName.isNotBlank()
                ) {
                    Text("Criar e Selecionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { inlineCreateType = null }) { Text("Cancelar") }
            }
        )
    }

    if (showAudioCaptureOptions) {
        AlertDialog(
            onDismissRequest = { showAudioCaptureOptions = false },
            title = { Text("Adicionar Áudio", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Como deseja adicionar o áudio à memória?", fontSize = 13.sp)

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAudioCaptureOptions = false
                                showAudioRecorderModal = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Gravar com Microfone", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Grave sua voz ou som ao vivo agora", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAudioCaptureOptions = false
                                audioPickerLauncher.launch("audio/*")
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Escolher Arquivo do Aparelho", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Músicas, podcasts ou arquivos salvos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAudioCaptureOptions = false }) { Text("Cancelar") }
            }
        )
    }

    if (showAudioRecorderModal) {
        AudioRecordingBottomSheet(
            onDismissRequest = { showAudioRecorderModal = false },
            onAudioRecorded = { audio ->
                attachments.add(
                    AttachmentInput(
                        type = "AUDIO",
                        uri = audio.uri.toString(),
                        durationMs = audio.durationMs,
                        waveformData = audio.waveformData,
                        mimeType = audio.mimeType
                    )
                )
            }
        )
    }

    if (showImportContactsInSheet) {
        ImportContactsDialog(
            onDismiss = { showImportContactsInSheet = false },
            onImportSelected = { contacts ->
                contacts.forEach { contact ->
                    onCreatePersonInline(
                        contact.name,
                        if (contact.phoneNumber != null) "Contato importado (${contact.phoneNumber})" else "Contato importado da agenda"
                    ) { newId ->
                        if (!selectedPersonIds.contains(newId)) {
                            selectedPersonIds.add(newId)
                        }
                    }
                }
                showImportContactsInSheet = false
            }
        )
    }
}
