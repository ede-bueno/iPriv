package com.example.priv.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.data.entity.GroupEntity
import com.example.priv.data.entity.MomentEntity
import com.example.priv.data.entity.PersonEntity
import com.example.priv.ui.audio.AudioPlayerManager
import com.example.priv.ui.components.MemoryCardItem
import com.example.priv.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToPerson: (String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMoments: () -> Unit = {},
    onNavigateToTimeline: () -> Unit = {},
    onOpenQuickCapture: () -> Unit,
    onOpenPreferences: (() -> Unit)? = null,
    onOpenAudioVault: (() -> Unit)? = null,
    onOpenAiAssistant: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeMemories by viewModel.activeMemories.collectAsState()
    val unorganizedMemories by viewModel.unorganizedMemories.collectAsState()
    val persons by viewModel.persons.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val moments by viewModel.moments.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val nostalgiaMemory by viewModel.randomNostalgiaMemory.collectAsState()

    val playbackState by AudioPlayerManager.playbackState.collectAsState()

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
                AudioPlayerManager.playOrPause(selectedMemoryForDetail!!.memory.id, attId, duration)
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Welcome & Quick Access Action Bar
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Sleek Greeting Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Seu Cofre de Histórias",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preservando vozes, momentos e afeto com privacidade total.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Keep btn_top_user_menu test tag available for test compatibility
                    FilledTonalIconButton(
                        onClick = { onOpenPreferences?.invoke() ?: onNavigateToProfile() },
                        modifier = Modifier
                            .size(42.dp)
                            .testTag("btn_top_user_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Menu do Usuário e Ajustes",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Quick Action Bar (nunca corta botões)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenAiAssistant?.invoke() }
                                .testTag("btn_open_ai_assistant")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Guardião IA Gemini",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Guardião IA ✨",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenPreferences?.invoke() ?: onNavigateToProfile() }
                                .testTag("btn_go_preferences")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Menu do Usuário",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Menu do Usuário ⚙️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onNavigateToProfile() }
                                .testTag("btn_go_profile")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Meu Perfil",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Meu Perfil 👤",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (onOpenAudioVault != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { onOpenAudioVault() }
                                    .testTag("btn_go_audio_vault")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "Cofre de Áudios",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Cofre de Áudios 🎙️",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onNavigateToMoments() }
                                .testTag("btn_go_moments")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = "Momentos & Coleções",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Momentos 🏷️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onNavigateToTimeline() }
                                .testTag("btn_go_timeline")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Linha & Canva ✨",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 1: "Olha o que apareceu..." (Flashback / Nostalgia)
        item {
            if (nostalgiaMemory != null) {
                val memory = nostalgiaMemory!!
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("nostalgia_banner")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Olha o que apareceu...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { viewModel.pickNewNostalgiaMemory() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Outra memória",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = memory.memory.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (memory.memory.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${memory.memory.note}\"",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (memory.primaryPerson != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = memory.primaryPerson.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val audioAttach = memory.attachments.firstOrNull { it.type == "AUDIO" }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "📅 Lembrança do dia",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (audioAttach != null) {
                                Button(
                                    onClick = {
                                        AudioPlayerManager.playOrPause(
                                            memory.memory.id,
                                            audioAttach.id,
                                            audioAttach.durationMs
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Ouvir agora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: "Continue ouvindo / de onde parou"
        if (playbackState.memoryId != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (playbackState.memoryId != null && playbackState.attachmentId != null) {
                                        AudioPlayerManager.playOrPause(
                                            playbackState.memoryId!!,
                                            playbackState.attachmentId!!,
                                            playbackState.totalDurationMs
                                        )
                                    }
                                },
                                modifier = Modifier.size(44.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Continue ouvindo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Tocando memória de áudio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${(playbackState.progress * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: "Continuar organizando" if unorganized memories exist
        if (unorganizedMemories.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onNavigateToLibrary() }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Continuar organizando",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${unorganizedMemories.size} memórias precisam de pessoa ou momento",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Section 4: "Pessoas com quem mais conversa"
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pessoas com quem conversa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(persons) { person ->
                        PersonStoryAvatarCard(
                            person = person,
                            onClick = { onNavigateToPerson(person.id) }
                        )
                    }
                }
            }
        }

        // Section 5: "Grupos recentes"
        if (groups.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grupos de memórias",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(groups) { group ->
                            GroupMiniCard(group = group)
                        }
                    }
                }
            }
        }

        // Section 6: "Momentos & Eventos recentes"
        if (moments.isNotEmpty() || collections.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Momentos & Coleções",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onNavigateToMoments,
                            modifier = Modifier.testTag("btn_see_all_moments")
                        ) {
                            Text(
                                text = "Ver todos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(moments) { moment ->
                            MomentMiniCard(
                                moment = moment,
                                onClick = onNavigateToMoments
                            )
                        }
                    }
                }
            }
        }

        // Section 7: Timeline of Recent Memories
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Linha do Tempo de Memórias",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (activeMemories.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Ainda sem memórias aqui.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Guarda o primeiro áudio ou foto antes que suma!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(activeMemories.take(15)) { memoryWithDetails ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MemoryCardItem(
                        memoryWithDetails = memoryWithDetails,
                        onToggleFavorite = { viewModel.toggleFavorite(memoryWithDetails) },
                        onMoveToTrash = { viewModel.moveToTrash(memoryWithDetails.memory.id) },
                        onPersonClick = onNavigateToPerson,
                        onCardClick = { selectedMemoryForDetail = memoryWithDetails }
                    )
                }
            }
        }
    }
}

@Composable
fun PersonStoryAvatarCard(
    person: PersonEntity,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .testTag("person_story_${person.id}")
    ) {
        Surface(
            shape = CircleShape,
            color = runCatching { Color(android.graphics.Color.parseColor(person.colorHex)) }
                .getOrDefault(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = person.name.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = person.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GroupMiniCard(group: GroupEntity) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(130.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = group.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (group.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = group.description,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MomentMiniCard(
    moment: MomentEntity,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
        modifier = Modifier
            .width(140.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("moment_card_${moment.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = moment.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (moment.date.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = moment.date,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}
