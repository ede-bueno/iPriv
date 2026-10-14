package com.example.priv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.ui.components.MemoryCardItem
import com.example.priv.ui.viewmodel.LibraryFilter
import com.example.priv.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onNavigateToPerson: (String) -> Unit,
    onNavigateToMoments: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val selectedSource by viewModel.selectedSource.collectAsState()
    val selectedEmotionTag by viewModel.selectedEmotionTag.collectAsState()
    val memories by viewModel.filteredMemories.collectAsState()

    var selectedMemoryForDetail by remember { mutableStateOf<com.example.priv.data.model.MemoryWithDetails?>(null) }

    if (selectedMemoryForDetail != null) {
        com.example.priv.ui.components.MemoryDetailModalSheet(
            memoryWithDetails = selectedMemoryForDetail!!,
            onDismiss = { selectedMemoryForDetail = null },
            onSaveEdits = { newTitle, newNote, tags, newAttachments, deletedAttachmentIds ->
                viewModel.updateMemoryFull(
                    memoryId = selectedMemoryForDetail!!.memory.id,
                    newTitle = newTitle,
                    newNote = newNote,
                    tags = tags,
                    newAttachments = newAttachments,
                    deletedAttachmentIds = deletedAttachmentIds,
                    onSuccess = { selectedMemoryForDetail = null }
                )
            },
            onMoveToTrash = {
                viewModel.moveToTrash(selectedMemoryForDetail!!.memory.id)
                selectedMemoryForDetail = null
            },
            onPlayAudio = { attId, duration ->
                com.example.priv.ui.audio.AudioPlayerManager.playOrPause(selectedMemoryForDetail!!.memory.id, attId, duration)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Biblioteca",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Todas as vozes e histórias em um só lugar",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TextButton(
                onClick = onNavigateToMoments,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Momentos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Buscar por pessoa, tag, palavra...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        // Main Type Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            item {
                FilterChip(
                    selected = activeFilter == LibraryFilter.ALL,
                    onClick = { viewModel.activeFilter.value = LibraryFilter.ALL },
                    label = { Text("Todas") }
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == LibraryFilter.FAVORITE,
                    onClick = { viewModel.activeFilter.value = LibraryFilter.FAVORITE },
                    label = { Text("Favoritas ❤️") }
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == LibraryFilter.AUDIO,
                    onClick = { viewModel.activeFilter.value = LibraryFilter.AUDIO },
                    label = { Text("Áudios 🎙️") }
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == LibraryFilter.UNORGANIZED,
                    onClick = { viewModel.activeFilter.value = LibraryFilter.UNORGANIZED },
                    label = { Text("Sem pessoa") }
                )
            }
            item {
                FilterChip(
                    selected = activeFilter == LibraryFilter.TRASH,
                    onClick = { viewModel.activeFilter.value = LibraryFilter.TRASH },
                    label = { Text("Lixeira 🗑️") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // App Source Filter Row (WhatsApp, Gravador, Importação)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            item {
                Text(
                    text = "Origem:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
            listOf("TODOS", "WhatsApp", "Gravador", "Importação").forEach { source ->
                item {
                    FilterChip(
                        selected = selectedSource == source,
                        onClick = { viewModel.selectedSource.value = source },
                        label = { Text(source, fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Emotion / Tag Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            item {
                Text(
                    text = "Emoção:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
            listOf("TODAS", "#engraçado", "#fofoca", "#rolê", "#conselho", "#familia").forEach { emotion ->
                item {
                    val isSel = selectedEmotionTag == emotion || (emotion == "TODAS" && selectedEmotionTag == "TODAS")
                    FilterChip(
                        selected = isSel,
                        onClick = { viewModel.selectedEmotionTag.value = emotion },
                        label = { Text(emotion, fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (activeFilter == LibraryFilter.TRASH) Icons.Default.DeleteOutline else Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (activeFilter == LibraryFilter.TRASH) "Lixeira vazia" else if (searchQuery.isNotBlank()) "Nenhuma memória encontrada para \"$searchQuery\"" else "Nenhuma memória guardada",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (activeFilter == LibraryFilter.TRASH) "Memórias excluídas permanecerão aqui antes da exclusão definitiva." else "Todas as suas vozes, momentos e histórias salvas ficam guardadas aqui.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(memories) { item ->
                    if (activeFilter == LibraryFilter.TRASH) {
                        TrashMemoryCardItem(
                            memoryWithDetails = item,
                            onRestore = { viewModel.restoreFromTrash(item.memory.id) },
                            onDeletePermanently = { viewModel.deletePermanently(item.memory.id) }
                        )
                    } else {
                        MemoryCardItem(
                            memoryWithDetails = item,
                            onToggleFavorite = { viewModel.toggleFavorite(item) },
                            onMoveToTrash = { viewModel.moveToTrash(item.memory.id) },
                            onPersonClick = onNavigateToPerson,
                            onCardClick = { selectedMemoryForDetail = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrashMemoryCardItem(
    memoryWithDetails: MemoryWithDetails,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val memory = memoryWithDetails.memory
    var showDeletePermanentConfirm by remember { mutableStateOf(false) }

    if (showDeletePermanentConfirm) {
        AlertDialog(
            onDismissRequest = { showDeletePermanentConfirm = false },
            title = {
                Text(
                    text = "Excluir definitivamente?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "A memória \"${memory.title.ifBlank { "Sem título" }}\" e todos os arquivos associados serão removidos permanentemente. Esta ação é irreversível.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeletePermanentConfirm = false
                        onDeletePermanently()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Excluir Definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePermanentConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = memory.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (memory.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = memory.note,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRestore,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recuperar", fontSize = 12.sp)
                }

                TextButton(
                    onClick = { showDeletePermanentConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Excluir definitivo", fontSize = 12.sp)
                }
            }
        }
    }
}
