package com.example.priv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.data.model.MemoryWithDetails
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReusableMemoryTimeline(
    memories: List<MemoryWithDetails>,
    onToggleFavorite: (MemoryWithDetails) -> Unit,
    onMoveToTrash: (String) -> Unit,
    emptyMessage: String = "Nenhuma memória encontrada neste filtro.",
    modifier: Modifier = Modifier
) {
    var memoryToDeleteId by remember { mutableStateOf<String?>(null) }

    if (memories.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = emptyMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    } else {
        // Group memories by date
        val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR")) }
        val groupedMemories = remember(memories) {
            memories.groupBy { memory ->
                dateFormat.format(Date(memory.memory.timestamp))
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            groupedMemories.forEach { (dateHeader, memoryGroup) ->
                item(key = "header_$dateHeader") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = dateHeader.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                items(
                    items = memoryGroup,
                    key = { it.memory.id }
                ) { memoryWithDetails ->
                    MemoryCardItem(
                        memoryWithDetails = memoryWithDetails,
                        onToggleFavorite = { onToggleFavorite(memoryWithDetails) },
                        onMoveToTrash = { memoryToDeleteId = memoryWithDetails.memory.id }
                    )
                }
            }
        }
    }

    // Confirmation dialog before deletion
    if (memoryToDeleteId != null) {
        AlertDialog(
            onDismissRequest = { memoryToDeleteId = null },
            title = { Text("Mover para a lixeira?", fontWeight = FontWeight.Bold) },
            text = { Text("A memória será movida para a lixeira. Seus arquivos e anexos não serão apagados permanentemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        memoryToDeleteId?.let { onMoveToTrash(it) }
                        memoryToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mover para lixeira")
                }
            },
            dismissButton = {
                TextButton(onClick = { memoryToDeleteId = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
