package com.example.priv.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.entity.ScrapbookEntity
import com.example.priv.data.entity.TimeCapsuleEntity
import com.example.priv.data.model.MemoryWithDetails
import com.example.priv.ui.viewmodel.TimelineScrapbookViewModel
import java.io.File
import java.util.Date
import kotlin.math.max
import kotlin.math.min

enum class TimelineScrapbookTab {
    CHRONOLOGICAL, RELATIONSHIPS, TIME_CAPSULES, SCRAPBOOK_CANVA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScrapbookScreen(
    viewModel: TimelineScrapbookViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.activeMemories.collectAsState()
    val persons by viewModel.persons.collectAsState()
    val timeCapsules by viewModel.timeCapsules.collectAsState()
    val scrapbooks by viewModel.scrapbooks.collectAsState()

    var selectedTab by remember { mutableStateOf(TimelineScrapbookTab.CHRONOLOGICAL) }
    var selectedPersonFilterId by remember { mutableStateOf<String?>(null) }

    var showCreateCapsuleDialog by remember { mutableStateOf(false) }
    var selectedCapsuleForDetail by remember { mutableStateOf<TimeCapsuleEntity?>(null) }
    var showCreateScrapbookDialog by remember { mutableStateOf(false) }
    var activeCanvasScrapbook by remember { mutableStateOf<ScrapbookEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Linha do Tempo & Scrapbook",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Preservação cronológica, cápsulas e colagens",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_timeline_back")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    when (selectedTab) {
                        TimelineScrapbookTab.TIME_CAPSULES -> {
                            FilledTonalButton(
                                onClick = { showCreateCapsuleDialog = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("btn_new_capsule"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nova Cápsula", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TimelineScrapbookTab.SCRAPBOOK_CANVA -> {
                            FilledTonalButton(
                                onClick = { showCreateScrapbookDialog = true },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("btn_new_scrapbook"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Novo Álbum", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Tab Selector Bar (Scrollable for responsiveness)
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == TimelineScrapbookTab.CHRONOLOGICAL,
                    onClick = { selectedTab = TimelineScrapbookTab.CHRONOLOGICAL },
                    text = { Text("Memórias (EXIF)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == TimelineScrapbookTab.RELATIONSHIPS,
                    onClick = { selectedTab = TimelineScrapbookTab.RELATIONSHIPS },
                    text = { Text("Relacionamentos", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == TimelineScrapbookTab.TIME_CAPSULES,
                    onClick = { selectedTab = TimelineScrapbookTab.TIME_CAPSULES },
                    text = { Text("Cápsulas do Tempo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == TimelineScrapbookTab.SCRAPBOOK_CANVA,
                    onClick = { selectedTab = TimelineScrapbookTab.SCRAPBOOK_CANVA },
                    text = { Text("Scrapbook Canva", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    TimelineScrapbookTab.CHRONOLOGICAL -> {
                        ChronologicalTimelineView(
                            memories = memories.sortedByDescending { it.memory.timestamp }
                        )
                    }

                    TimelineScrapbookTab.RELATIONSHIPS -> {
                        RelationshipTimelineView(
                            memories = memories,
                            persons = persons,
                            selectedPersonId = selectedPersonFilterId,
                            onSelectPerson = { selectedPersonFilterId = it }
                        )
                    }

                    TimelineScrapbookTab.TIME_CAPSULES -> {
                        TimeCapsulesView(
                            capsules = timeCapsules,
                            onUnlock = { viewModel.unlockCapsule(it) },
                            onDelete = { viewModel.deleteCapsule(it) },
                            onCapsuleClick = { selectedCapsuleForDetail = it },
                            onCreateClick = { showCreateCapsuleDialog = true }
                        )
                    }
                    TimelineScrapbookTab.SCRAPBOOK_CANVA -> {
                        if (activeCanvasScrapbook != null) {
                            ScrapbookInteractiveCanvas(
                                scrapbook = activeCanvasScrapbook!!,
                                memories = memories,
                                onClose = { activeCanvasScrapbook = null }
                            )
                        } else {
                            ScrapbooksGalleryView(
                                scrapbooks = scrapbooks,
                                onOpenCanvas = { activeCanvasScrapbook = it },
                                onCreateClick = { showCreateScrapbookDialog = true },
                                onDelete = { viewModel.deleteScrapbook(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateCapsuleDialog) {
        CreateTimeCapsuleFullScreenDialog(
            memories = memories,
            persons = persons,
            onDismiss = { showCreateCapsuleDialog = false },
            onConfirm = { title, note, days, selMemories, mediaItems, personId, colorHex ->
                viewModel.createTimeCapsule(title, note, days, selMemories, mediaItems, personId, colorHex)
                showCreateCapsuleDialog = false
            }
        )
    }

    if (selectedCapsuleForDetail != null) {
        val currentCapsule = timeCapsules.find { it.id == selectedCapsuleForDetail!!.id } ?: selectedCapsuleForDetail!!
        val linkedMemories = memories.filter { currentCapsule.memoryIdsJson.contains(it.memory.id) }
        val targetPerson = persons.find { it.id == currentCapsule.targetPersonId }
        CapsuleDetailModalSheet(
            capsule = currentCapsule,
            linkedMemories = linkedMemories,
            person = targetPerson,
            onDismiss = { selectedCapsuleForDetail = null },
            onUnlock = {
                viewModel.unlockCapsule(currentCapsule.id)
            }
        )
    }
    if (showCreateScrapbookDialog) {
        CreateScrapbookDialog(
            onDismiss = { showCreateScrapbookDialog = false },
            onConfirm = { title, desc, theme, emoji, stickers ->
                viewModel.createScrapbook(title, desc, theme, emoji, stickers)
                showCreateScrapbookDialog = false
            }
        )
    }
}

/**
 * Visualizador Interativo com Suporte a Zoom In, Zoom Out, Pan e Colagem tipo Canva
 */
@Composable
fun ScrapbookInteractiveCanvas(
    scrapbook: ScrapbookEntity,
    memories: List<MemoryWithDetails>,
    onClose: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Obter mídias para compor na colagem
    val imageAttachments = remember(memories) {
        memories.flatMap { mem ->
            mem.attachments.filter { it.type == "IMAGE" }.map { att ->
                att to mem.memory.title
            }
        }.take(16)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (scrapbook.backgroundPattern) {
                    "DARK_NEBULA" -> Color(0xFF0F0C1E)
                    "PASTEL_CREAM" -> Color(0xFFFFF9E6)
                    else -> Color(0xFF141220)
                }
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.4f, 4.0f)
                    offset = Offset(
                        x = offset.x + pan.x,
                        y = offset.y + pan.y
                    )
                }
            }
    ) {
        // Floating Controls Toolbar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar Canva", tint = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    text = "${scrapbook.coverEmojiOrIcon} ${scrapbook.title}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { scale = max(0.5f, scale - 0.25f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Diminuir zoom")
                }
                Text(
                    text = "${(scale * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { scale = min(3.5f, scale + 0.25f) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Aumentar zoom")
                }
                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Resetar visão")
                }
            }
        }

        // Infinite Zoomable & Pannable Canvas Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentAlignment = Alignment.Center
        ) {
            // Collage Board Center Area
            Column(
                modifier = Modifier
                    .width(820.dp)
                    .wrapContentHeight()
                    .padding(32.dp)
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(32.dp)
                    )
                    .border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        RoundedCornerShape(32.dp)
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${scrapbook.coverEmojiOrIcon} ${scrapbook.title}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                if (scrapbook.description.isNotBlank()) {
                    Text(
                        text = scrapbook.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }

                // Decorative Y2K / Aesthetic Stickers Bar
                Row(
                    modifier = Modifier.padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("💖", "⚡", "✨", "🎵", "📷", "⭐", "🍀", "🫧").forEach { sticker ->
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(sticker, fontSize = 18.sp)
                            }
                        }
                    }
                }

                // Polaroid & Aesthetic Pin Grid Collage
                if (imageAttachments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhuma foto salva ainda.\nAdicione fotos pelo botão Guardar Memória para compor esta colagem!",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val rotations = listOf(-4f, 3f, -2f, 5f, -3f, 2f, -5f, 4f)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        imageAttachments.chunked(3).forEachIndexed { rowIndex, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowItems.forEachIndexed { itemIndex, (attachment, title) ->
                                    val rot = rotations[(rowIndex * 3 + itemIndex) % rotations.size]
                                    PolaroidPhotoItem(
                                        imagePath = attachment.localPath.ifBlank { attachment.uri },
                                        title = title,
                                        rotationDeg = rot
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pinch hint badge at bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                text = "💡 Pinça para Zoom e arraste para navegar pelo Canva",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun PolaroidPhotoItem(
    imagePath: String,
    title: String,
    rotationDeg: Float
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .width(210.dp)
            .rotate(rotationDeg)
            .shadow(10.dp, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFFFDF5) // Vintage polaroid ivory paper
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imgFile = remember(imagePath) { File(imagePath) }
            val imgModel = if (imgFile.exists()) imgFile else imagePath

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imgModel)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title.ifBlank { "Momento GuardDay" },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C243B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Linha do tempo cronológica com destaque para a data original de criação (EXIF / Metadados)
 */
@Composable
fun ChronologicalTimelineView(
    memories: List<MemoryWithDetails>
) {
    if (memories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nenhuma memória registrada na linha do tempo ainda.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timeline_chronological_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Memórias ordenadas com precisão pela data original extraída dos metadados das fotos ou da data fixada por você.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        items(memories, key = { it.memory.id }) { item ->
            TimelineMemoryNodeItem(item = item)
        }
    }
}

@Composable
fun TimelineMemoryNodeItem(item: MemoryWithDetails) {
    val dateStr = remember(item.memory.timestamp) {
        DateFormat.format("dd 'de' MMMM 'de' yyyy, HH:mm", Date(item.memory.timestamp)).toString()
    }

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Vertical Timeline Spine
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f, fill = false)
                    .height(90.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Content Card
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.memory.source,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.memory.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (item.memory.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.memory.note,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item.attachments.take(3).forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (att.type) {
                                            "IMAGE" -> Icons.Default.Image
                                            "AUDIO" -> Icons.Default.GraphicEq
                                            "VIDEO" -> Icons.Default.Videocam
                                            else -> Icons.Default.AttachFile
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = att.type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
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

/**
 * Linha do tempo focada em Relacionamentos (amizades, amores, familiares, conexões)
 */
@Composable
fun RelationshipTimelineView(
    memories: List<MemoryWithDetails>,
    persons: List<PersonEntity>,
    selectedPersonId: String?,
    onSelectPerson: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Person filter bubbles
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedPersonId == null,
                    onClick = { onSelectPerson(null) },
                    label = { Text("Todos os vínculos") }
                )
            }
            items(persons, key = { it.id }) { person ->
                FilterChip(
                    selected = selectedPersonId == person.id,
                    onClick = { onSelectPerson(if (selectedPersonId == person.id) null else person.id) },
                    label = { Text("${person.name} (${person.relationship})") }
                )
            }
        }

        val filteredMemories = remember(memories, selectedPersonId) {
            if (selectedPersonId == null) {
                memories.filter { it.primaryPerson != null || it.taggedPersons.isNotEmpty() }
            } else {
                memories.filter { mem ->
                    mem.primaryPerson?.id == selectedPersonId || mem.taggedPersons.any { it.id == selectedPersonId }
                }
            }
        }

        if (filteredMemories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma memória associada a este relacionamento ainda.\nMarque a pessoa ao guardar uma nova memória!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMemories, key = { it.memory.id }) { mem ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = mem.primaryPerson?.name ?: mem.taggedPersons.firstOrNull()?.name ?: "Vínculo Especial",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = DateFormat.format("dd/MM/yyyy", Date(mem.memory.timestamp)).toString(),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = mem.memory.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            if (mem.memory.note.isNotBlank()) {
                                Text(text = mem.memory.note, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visualização e gestão de Cápsulas do Tempo
 */
@Composable
fun TimeCapsulesView(
    capsules: List<TimeCapsuleEntity>,
    onUnlock: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCapsuleClick: (TimeCapsuleEntity) -> Unit,
    onCreateClick: () -> Unit
) {
    if (capsules.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhuma Cápsula do Tempo criada ainda.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Feche um conjunto de memórias e recortes para serem abertos somente no futuro!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Criar Primeira Cápsula")
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(capsules, key = { it.id }) { capsule ->
            val now = System.currentTimeMillis()
            val isReadyToUnlock = now >= capsule.unlockTimestamp
            Card(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable { onCapsuleClick(capsule) },
                colors = CardDefaults.cardColors(
                    containerColor = if (capsule.isLocked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (capsule.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (capsule.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = capsule.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { onDelete(capsule.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (capsule.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = capsule.note,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val unlockDateStr = DateFormat.format("dd/MM/yyyy 'às' HH:mm", Date(capsule.unlockTimestamp)).toString()

                    if (capsule.isLocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isReadyToUnlock) "Tempo atingido! Pronta para ser aberta." else "Abertura programada: $unlockDateStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onUnlock(capsule.id) },
                            enabled = isReadyToUnlock,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isReadyToUnlock) "Abrir Cápsula Agora" else "Bloqueada no Tempo")
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✨ Cápsula Aberta! Toque para ver o repositório desvendado.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Galeria de Scrapbooks e Álbuns Estéticos
 */
@Composable
fun ScrapbooksGalleryView(
    scrapbooks: List<ScrapbookEntity>,
    onOpenCanvas: (ScrapbookEntity) -> Unit,
    onCreateClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    if (scrapbooks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhum Scrapbook criado ainda.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Crie álbuns estéticos e colagens interativas no estilo Canva com zoom e navegação livre!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Criar Primeiro Scrapbook")
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(scrapbooks, key = { it.id }) { book ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenCanvas(book) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = book.coverEmojiOrIcon, fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (book.description.isNotBlank()) {
                            Text(
                                text = book.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Estilo: ${book.styleTheme} • Toque para abrir no Canva",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { onDelete(book.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * Diálogo para Criação de Cápsula do Tempo
 */
@Composable
fun CreateScrapbookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, styleTheme: String, emoji: String, stickers: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var styleTheme by remember { mutableStateOf("NEO_Y2K") }
    var selectedEmoji by remember { mutableStateOf("✨") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Scrapbook Canva", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Scrapbook") },
                    placeholder = { Text("Ex: Nossas Viagens de Férias") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição / Frase de Abertura") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Ícone de Capa:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("✨", "💖", "🎸", "🎧", "📷", "⭐", "🍀").forEach { em ->
                        FilterChip(
                            selected = selectedEmoji == em,
                            onClick = { selectedEmoji = em },
                            label = { Text(em, fontSize = 16.sp) }
                        )
                    }
                }

                Text("Estilo Visual:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("NEO_Y2K" to "Neo Y2K", "POLAROID" to "Polaroid", "JOURNAL" to "Journal").forEach { (key, label) ->
                        FilterChip(
                            selected = styleTheme == key,
                            onClick = { styleTheme = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, desc, styleTheme, selectedEmoji, listOf("💖", "⚡", "✨", "🎵"))
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Criar Álbum")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
