package com.example.priv.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val memories by viewModel.filteredMemories.collectAsState()

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
            Column {
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
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Buscar por pessoa, tag, palavra...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input"),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row
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

        Spacer(modifier = Modifier.height(12.dp))

        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (activeFilter == LibraryFilter.TRASH) "Lixeira vazia" else "Nenhuma memória encontrada",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                            onPersonClick = onNavigateToPerson
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
                    onClick = onDeletePermanently,
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
