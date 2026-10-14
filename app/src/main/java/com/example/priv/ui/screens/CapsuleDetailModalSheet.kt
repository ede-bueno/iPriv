package com.example.priv.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.entity.TimeCapsuleEntity
import com.example.priv.data.model.CapsuleMediaItem
import com.example.priv.data.model.MemoryWithDetails
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapsuleDetailModalSheet(
    capsule: TimeCapsuleEntity,
    linkedMemories: List<MemoryWithDetails>,
    person: PersonEntity?,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    val modalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val now = System.currentTimeMillis()
    val isReadyToUnlock = now >= capsule.unlockTimestamp
    val unlockDateStr = DateFormat.format("dd/MM/yyyy 'às' HH:mm", Date(capsule.unlockTimestamp)).toString()
    val createdDateStr = DateFormat.format("dd/MM/yyyy", Date(capsule.createdAt)).toString()

    val directMediaItems = remember(capsule.mediaItemsJson) {
        CapsuleMediaItem.listFromJsonString(capsule.mediaItemsJson)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalBottomSheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("capsule_detail_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Status Badge & Titulo
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (capsule.isLocked) {
                                if (isReadyToUnlock) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            } else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (!capsule.isLocked) Icons.Default.LockOpen else if (isReadyToUnlock) Icons.Default.Key else Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (!capsule.isLocked) MaterialTheme.colorScheme.primary else if (isReadyToUnlock) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (!capsule.isLocked) "CÁPSULA ABERTA" else if (isReadyToUnlock) "PRONTA PARA ABRIR" else "TRANCADA NO TEMPO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!capsule.isLocked) MaterialTheme.colorScheme.primary else if (isReadyToUnlock) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (person != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(person.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = capsule.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Criada em $createdDateStr • Desbloqueio: $unlockDateStr",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ação de Desbloqueio se pronta ou mensagem se trancada
            item {
                if (capsule.isLocked) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isReadyToUnlock) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isReadyToUnlock) Icons.Default.Key else Icons.Default.HourglassTop,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = if (isReadyToUnlock) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isReadyToUnlock) "O tempo estipulado chegou!" else "Esta cápsula está selada hermeticamente.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isReadyToUnlock)
                                    "Você já pode quebrar o lacre e desvendar todas as memórias, fotos, vídeos e músicas guardadas."
                                else
                                    "O conteúdo selecionado permanecerá confidencial e intocado até a data prevista ($unlockDateStr).",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            if (isReadyToUnlock) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onUnlock,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("unlock_capsule_sheet_btn")
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Quebrar Lacre e Abrir Cápsula", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Lacre Aberto!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                Text("Todas as lembranças do passado foram restauradas para você reviver.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Carta / Mensagem Secreta
            if (capsule.note.isNotBlank()) {
                item {
                    Column {
                        Text("📜 Carta & Mensagem", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Text(
                                text = if (capsule.isLocked && !isReadyToUnlock) "🔒 Mensagem oculta até o desbloqueio." else capsule.note,
                                fontSize = 13.sp,
                                color = if (capsule.isLocked && !isReadyToUnlock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }

            // Repositório de Mídias Específicas da Cápsula (Fotos, Vídeos, Músicas, Textos, Colagens)
            if (directMediaItems.isNotEmpty()) {
                item {
                    Text(
                        text = "📦 Repositório da Cápsula (${directMediaItems.size} itens)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(directMediaItems) { item ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val icon = when (item.type) {
                                "IMAGE" -> Icons.Default.Image
                                "VIDEO" -> Icons.Default.Videocam
                                "AUDIO" -> Icons.Default.MusicNote
                                "COLLAGE" -> Icons.Default.AutoAwesome
                                else -> Icons.Default.Article
                            }
                            val tint = when (item.type) {
                                "IMAGE" -> Color(0xFF4CAF50)
                                "VIDEO" -> Color(0xFFE91E63)
                                "AUDIO" -> Color(0xFFFF9800)
                                "COLLAGE" -> Color(0xFF9C27B0)
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tint.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = tint)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title.ifBlank { item.type },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                if (item.extraMetadata.isNotBlank()) {
                                    Text(
                                        text = item.extraMetadata,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (capsule.isLocked && !isReadyToUnlock) {
                                    Text(
                                        text = "🔒 Bloqueado no cofre temporal",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (item.textContent.isNotBlank()) {
                                    Text(
                                        text = item.textContent,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Memórias do GuardDay Vinculadas
            if (linkedMemories.isNotEmpty()) {
                item {
                    Text(
                        text = "🧠 Memórias Guardadas Incluídas (${linkedMemories.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(linkedMemories) { mem ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (capsule.isLocked && !isReadyToUnlock) Icons.Default.Lock else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (capsule.isLocked && !isReadyToUnlock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (capsule.isLocked && !isReadyToUnlock) "Memória Segredo (Oculta)" else mem.memory.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (capsule.isLocked && !isReadyToUnlock)
                                        "Lacre temporal ativo"
                                    else
                                        "${mem.attachments.size} anexo(s) • ${mem.memory.source}",
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
