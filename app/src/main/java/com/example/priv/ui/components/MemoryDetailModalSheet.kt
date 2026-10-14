package com.example.priv.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.priv.data.entity.MemoryAttachmentEntity
import com.example.priv.data.model.AttachmentInput
import com.example.priv.data.model.MemoryWithDetails

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailModalSheet(
    memoryWithDetails: MemoryWithDetails,
    onDismiss: () -> Unit,
    onSaveEdits: (
        newTitle: String,
        newNote: String,
        tags: List<String>,
        newAttachments: List<AttachmentInput>,
        deletedAttachmentIds: List<String>
    ) -> Unit,
    onMoveToTrash: () -> Unit,
    onPlayAudio: (attachmentId: String, durationMs: Long) -> Unit
) {
    val memory = memoryWithDetails.memory
    var isEditing by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(memory.title) }
    var note by remember { mutableStateOf(memory.note) }
    var tagsString by remember { mutableStateOf(memoryWithDetails.tags.joinToString(", ") { it.name }) }

    val newAttachments = remember { mutableStateListOf<AttachmentInput>() }
    val deletedAttachmentIds = remember { mutableStateListOf<String>() }

    var selectedPreviewImage by remember { mutableStateOf<String?>(null) }
    var selectedPreviewVideo by remember { mutableStateOf<String?>(null) }

    // Media Launchers to add more files to existing memory
    val addPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            newAttachments.add(
                AttachmentInput(
                    type = "IMAGE",
                    uri = uri.toString(),
                    mimeType = "image/jpeg"
                )
            )
        }
    }

    val addVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newAttachments.add(
                AttachmentInput(
                    type = "VIDEO",
                    uri = it.toString(),
                    mimeType = "video/mp4"
                )
            )
        }
    }

    val addAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newAttachments.add(
                AttachmentInput(
                    type = "AUDIO",
                    uri = it.toString(),
                    durationMs = 30000L,
                    waveformData = "0.4,0.7,0.5,0.9,0.3,0.8,0.6",
                    mimeType = "audio/ogg"
                )
            )
        }
    }

    if (selectedPreviewImage != null) {
        ImageViewerDialog(
            imageUriOrPath = selectedPreviewImage!!,
            title = memory.title,
            note = memory.note,
            onDismiss = { selectedPreviewImage = null }
        )
    }

    if (selectedPreviewVideo != null) {
        VideoViewerDialog(
            videoUriOrPath = selectedPreviewVideo!!,
            title = memory.title,
            onDismiss = { selectedPreviewVideo = null }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
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
                .padding(horizontal = 20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isEditing) "Editando Memória" else "Memória GuardDay",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEditing) "Modifique ou anexe novos arquivos" else "Preservada com contexto",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isEditing) {
                        Button(
                            onClick = {
                                val cleanTags = tagsString.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }
                                onSaveEdits(
                                    title,
                                    note,
                                    cleanTags,
                                    newAttachments.toList(),
                                    deletedAttachmentIds.toList()
                                )
                                isEditing = false
                            },
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salvar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title and Note section
                item {
                    if (isEditing) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Título da Memória") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("História e notas") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            minLines = 3,
                            maxLines = 6
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Tag Chips (família, viagem, trabalho)
                        Text(
                            text = "Tags rápidas:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("família", "viagem", "trabalho").forEach { quickTag ->
                                val currentTags = tagsString.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
                                val isSelected = quickTag.lowercase() in currentTags
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val mutable = currentTags.toMutableList()
                                        if (isSelected) {
                                            mutable.remove(quickTag.lowercase())
                                        } else {
                                            mutable.add(quickTag.lowercase())
                                        }
                                        tagsString = mutable.joinToString(", ")
                                    },
                                    label = { Text("#$quickTag", fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = tagsString,
                            onValueChange = { tagsString = it },
                            label = { Text("Tags (separadas por vírgula)") },
                            placeholder = { Text("ex: família, viagem, trabalho") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = memory.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (memory.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = memory.note,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp
                            )
                        }

                        // Badges de Tags em modo de visualização
                        val displayTags = if (memory.tags.isNotBlank()) {
                            memory.tags.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
                        } else {
                            memoryWithDetails.tags.map { it.name.trim().removePrefix("#") }.filter { it.isNotEmpty() }
                        }
                        if (displayTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                displayTags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (tag.lowercase()) {
                                            "família", "familia" -> Color(0xFFE8F5E9)
                                            "viagem" -> Color(0xFFE3F2FD)
                                            "trabalho" -> Color(0xFFFFF3E0)
                                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        }
                                    ) {
                                        Text(
                                            text = "#$tag",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (tag.lowercase()) {
                                                "família", "familia" -> Color(0xFF2E7D32)
                                                "viagem" -> Color(0xFF1565C0)
                                                "trabalho" -> Color(0xFFE65100)
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Person association info
                if (memoryWithDetails.primaryPerson != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            runCatching { Color(android.graphics.Color.parseColor(memoryWithDetails.primaryPerson.colorHex)) }
                                                .getOrDefault(MaterialTheme.colorScheme.primary)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = memoryWithDetails.primaryPerson.name.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = memoryWithDetails.primaryPerson.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (memoryWithDetails.primaryPerson.relationship.isNotBlank()) {
                                        Text(
                                            text = memoryWithDetails.primaryPerson.relationship,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Attachments Management Box
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
                            val totalCount = memoryWithDetails.attachments.count { it.id !in deletedAttachmentIds } + newAttachments.size
                            Text(
                                text = "Arquivos da Memória ($totalCount)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (isEditing) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = { addPhotoLauncher.launch("image/*") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "+ Foto", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { addVideoLauncher.launch("video/*") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.VideoCall, contentDescription = "+ Vídeo", tint = MaterialTheme.colorScheme.secondary)
                                    }
                                    IconButton(
                                        onClick = { addAudioLauncher.launch("audio/*") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Mic, contentDescription = "+ Áudio", tint = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Existing Attachments list
                        val visibleExisting = memoryWithDetails.attachments.filterNot { it.id in deletedAttachmentIds }
                        if (visibleExisting.isNotEmpty() || newAttachments.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(visibleExisting) { att ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                            .clickable {
                                                when (att.type) {
                                                    "IMAGE" -> selectedPreviewImage = if (att.localPath.isNotBlank()) att.localPath else att.uri
                                                    "VIDEO" -> selectedPreviewVideo = if (att.localPath.isNotBlank()) att.localPath else att.uri
                                                    "AUDIO" -> onPlayAudio(att.id, att.durationMs)
                                                }
                                            }
                                    ) {
                                        when (att.type) {
                                            "IMAGE" -> {
                                                AsyncImage(
                                                    model = if (att.localPath.isNotBlank()) att.localPath else att.uri,
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
                                                    Icon(Icons.Default.PlayCircle, contentDescription = "Vídeo", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                                                }
                                            }
                                            "AUDIO" -> {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.GraphicEq, contentDescription = "Áudio", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                                        Text("Play", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                            else -> {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                ) {
                                                    Icon(Icons.Default.Description, contentDescription = "Arquivo", tint = MaterialTheme.colorScheme.tertiary)
                                                }
                                            }
                                        }

                                        if (isEditing) {
                                            IconButton(
                                                onClick = { deletedAttachmentIds.add(att.id) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .padding(2.dp)
                                                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Excluir anexo", tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }

                                // Newly staged attachments
                                items(newAttachments.toList()) { newAtt ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                    ) {
                                        when (newAtt.type) {
                                            "IMAGE" -> {
                                                AsyncImage(
                                                    model = newAtt.uri,
                                                    contentDescription = "Nova Foto",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "VIDEO" -> {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            else -> {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { newAttachments.remove(newAtt) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .padding(2.dp)
                                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remover", tint = Color.White, modifier = Modifier.size(12.dp))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                        ) {
                                            Text("Novo", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Danger zone: Move to Trash
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            onMoveToTrash()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mover esta memória para a Lixeira", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
