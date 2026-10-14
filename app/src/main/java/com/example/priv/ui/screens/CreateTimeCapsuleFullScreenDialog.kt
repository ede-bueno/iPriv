package com.example.priv.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.model.CapsuleMediaItem
import com.example.priv.data.model.MemoryWithDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTimeCapsuleFullScreenDialog(
    memories: List<MemoryWithDetails>,
    persons: List<PersonEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        note: String,
        daysInFuture: Int,
        memoryIds: List<String>,
        mediaItems: List<CapsuleMediaItem>,
        targetPersonId: String?,
        themeColorHex: String
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var daysInFuture by remember { mutableIntStateOf(30) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    var selectedColorHex by remember { mutableStateOf("#7C4DFF") }

    // Memórias Selecionadas
    val selectedMemoryIds = remember { mutableStateListOf<String>() }

    // Mídias Diretas da Cápsula (Repositório)
    val directMediaItems = remember { mutableStateListOf<CapsuleMediaItem>() }

    // Dialogo interno para adicionar mídia
    var showAddMediaDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Nova Cápsula do Tempo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Insira um título para a cápsula!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onConfirm(
                                    title,
                                    note,
                                    daysInFuture,
                                    selectedMemoryIds.toList(),
                                    directMediaItems.toList(),
                                    selectedPersonId,
                                    selectedColorHex
                                )
                            },
                            enabled = title.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp).testTag("save_capsule_btn")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Trancar Cápsula")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner explicativo
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "As mídias e recortes desta cápsula ficarão bloqueados com lacre temporal inviolável até a data programada.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 1. Dados Básicos
                item {
                    Text("1. Identificação", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nome da Cápsula") },
                        placeholder = { Text("Ex: Para mim daqui a 1 ano, Turma do Terceirão...") },
                        modifier = Modifier.fillMaxWidth().testTag("capsule_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Carta / Recado do Passado") },
                        placeholder = { Text("Escreva o que você está sentindo hoje ou uma mensagem para o futuro...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // 2. Data de Desbloqueio (Agendamento)
                item {
                    Text("2. Agendamento de Abertura", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val options = listOf(
                            7 to "7 dias",
                            30 to "1 mês",
                            90 to "3 meses",
                            180 to "6 meses",
                            365 to "1 ano",
                            730 to "2 anos"
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(options) { (days, label) ->
                                FilterChip(
                                    selected = daysInFuture == days,
                                    onClick = { daysInFuture = days },
                                    label = { Text(label, fontSize = 12.sp) },
                                    leadingIcon = if (daysInFuture == days) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // 3. Pessoa Alvo / Destinatário (Opcional)
                if (persons.isNotEmpty()) {
                    item {
                        Text("3. Dedicar para alguém especial (Opcional)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedPersonId == null,
                                    onClick = { selectedPersonId = null },
                                    label = { Text("Para Mim / Geral", fontSize = 12.sp) }
                                )
                            }
                            items(persons) { p ->
                                FilterChip(
                                    selected = selectedPersonId == p.id,
                                    onClick = { selectedPersonId = p.id },
                                    label = { Text(p.name, fontSize = 12.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. Repositório de Conteúdo da Cápsula (Fotos, Vídeos, Músicas, Textos, Colagens)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("4. Repositório de Conteúdo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Adicione fotos, vídeos, músicas ou recortes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalButton(
                            onClick = { showAddMediaDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar Mídia", fontSize = 12.sp)
                        }
                    }

                    if (directMediaItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            directMediaItems.forEachIndexed { index, media ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val icon = when (media.type) {
                                            "IMAGE" -> Icons.Default.Image
                                            "VIDEO" -> Icons.Default.Videocam
                                            "AUDIO" -> Icons.Default.MusicNote
                                            "COLLAGE" -> Icons.Default.AutoAwesome
                                            else -> Icons.Default.Article
                                        }
                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(media.title.ifBlank { media.type }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            if (media.extraMetadata.isNotBlank()) {
                                                Text(media.extraMetadata, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        IconButton(
                                            onClick = { directMediaItems.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Vincular Grupo de Memórias Existentes
                if (memories.isNotEmpty()) {
                    item {
                        Text("5. Selecionar Memórias Salvas (${selectedMemoryIds.size}/${memories.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            memories.take(10).forEach { mem ->
                                val isSelected = selectedMemoryIds.contains(mem.memory.id)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedMemoryIds.remove(mem.memory.id)
                                            else selectedMemoryIds.add(mem.memory.id)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedMemoryIds.add(mem.memory.id)
                                                else selectedMemoryIds.remove(mem.memory.id)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mem.memory.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(
                                                text = "${mem.attachments.size} anexo(s) • ${mem.memory.source}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal simples para adicionar Fotos, Vídeos, Músicas ou Textos
    if (showAddMediaDialog) {
        AddCapsuleMediaDialog(
            onDismiss = { showAddMediaDialog = false },
            onAdd = { item ->
                directMediaItems.add(item)
                showAddMediaDialog = false
            }
        )
    }
}

@Composable
fun AddCapsuleMediaDialog(
    onDismiss: () -> Unit,
    onAdd: (CapsuleMediaItem) -> Unit
) {
    var type by remember { mutableStateOf("IMAGE") }
    var title by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    var extraMetadata by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adicionar Item ao Repositório", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tipo de Mídia:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = listOf(
                        "IMAGE" to "Foto 📸",
                        "VIDEO" to "Vídeo 🎥",
                        "AUDIO" to "Música/Áudio 🎵",
                        "TEXT" to "Texto/Recado 📝",
                        "COLLAGE" to "Colagem/Scrap 🎨"
                    )
                    items(types) { (t, label) ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                if (t == "AUDIO" && extraMetadata.isBlank()) extraMetadata = "Música tema do momento"
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título ou Descrição") },
                    placeholder = { Text("Ex: Nossa música favorita, Foto do almoço...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (type == "TEXT" || type == "COLLAGE") {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        label = { Text("Conteúdo do Texto / Notas") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (type == "AUDIO" || type == "VIDEO") {
                    OutlinedTextField(
                        value = extraMetadata,
                        onValueChange = { extraMetadata = it },
                        label = { Text(if (type == "AUDIO") "Faixa / Artista / Duração" else "Detalhes do Vídeo") },
                        placeholder = { Text(if (type == "AUDIO") "Ex: Oasis - Wonderwall (4:18)" else "Ex: Gravado no aniversário") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        CapsuleMediaItem(
                            type = type,
                            title = title.ifBlank { "Arquivo $type" },
                            textContent = textContent,
                            extraMetadata = extraMetadata
                        )
                    )
                },
                enabled = title.isNotBlank() || textContent.isNotBlank() || extraMetadata.isNotBlank()
            ) {
                Text("Inserir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
