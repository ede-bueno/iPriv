package com.example.priv.ui.screens

import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
                            text = "Linha do Tempo & Memórias",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Suas histórias em ordem, cápsulas do tempo e murais afetivos",
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
                    text = { Text("Por Data", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == TimelineScrapbookTab.RELATIONSHIPS,
                    onClick = { selectedTab = TimelineScrapbookTab.RELATIONSHIPS },
                    text = { Text("Por Pessoa", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == TimelineScrapbookTab.TIME_CAPSULES,
                    onClick = { selectedTab = TimelineScrapbookTab.TIME_CAPSULES },
                    text = { Text("Cápsulas do Futuro", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == TimelineScrapbookTab.SCRAPBOOK_CANVA,
                    onClick = { selectedTab = TimelineScrapbookTab.SCRAPBOOK_CANVA },
                    text = { Text("Álbum de Memórias", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
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
                                onSaveItems = { updatedItems ->
                                    viewModel.saveScrapbookItems(activeCanvasScrapbook!!.id, updatedItems)
                                    activeCanvasScrapbook = activeCanvasScrapbook!!.copy(
                                        itemsJson = com.example.priv.data.model.ScrapbookPageItem.serializeList(updatedItems)
                                    )
                                },
                                onUpdateTheme = { newPattern ->
                                    val updated = activeCanvasScrapbook!!.copy(
                                        backgroundPattern = newPattern
                                    )
                                    viewModel.updateScrapbook(updated)
                                    activeCanvasScrapbook = updated
                                },
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
 * Salva permanentemente uma cópia da foto escolhida no armazenamento interno do app
 * para garantir que o álbum nunca perca o acesso à imagem ou sofra expiração de permissão de URI.
 */
private fun copyUriToScrapbookStorage(context: android.content.Context, uri: Uri): String {
    return try {
        val storageDir = File(context.filesDir, "scrapbook_images")
        if (!storageDir.exists()) storageDir.mkdirs()
        val destFile = File(storageDir, "scrapbook_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(6)}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (e: Exception) {
        uri.toString()
    }
}

/**
 * Mural Interativo do Álbum de Memórias com suporte a fotos da galeria, fotos salvas no app,
 * bilhetes coloridos, adesivos afetivos e cartões temáticos que guiam a criação.
 */
@Composable
fun ScrapbookInteractiveCanvas(
    scrapbook: ScrapbookEntity,
    memories: List<MemoryWithDetails>,
    onSaveItems: (List<com.example.priv.data.model.ScrapbookPageItem>) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Itens persistidos do Álbum
    var itemsList by remember(scrapbook.id, scrapbook.itemsJson) {
        val parsed = com.example.priv.data.model.ScrapbookPageItem.parseList(scrapbook.itemsJson)
        val initialList = if (parsed.isNotEmpty()) {
            parsed
        } else if (scrapbook.id == "seed_scrapbook_2" || scrapbook.title.contains("Melhores Amigos", ignoreCase = true)) {
            // Template Modelo dos Melhores Amigos com ações reais e dinâmicas
            listOf(
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "bf_slot_1",
                    type = com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT,
                    title = "Foto com Meu Melhor Amigo(a) 📸",
                    actionPrompt = "Toque aqui para escolher a melhor foto de vocês juntos!",
                    rotationDeg = -2.5f
                ),
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "bf_note_1",
                    type = com.example.priv.data.model.ScrapbookItemType.NOTE,
                    title = "Regra de Ouro da Amizade 💜",
                    uriOrContent = "Parceria em todos os momentos: rir junto até a barriga doer, desabafar nas horas difíceis e nunca deixar uma mensagem sem resposta!",
                    noteColorHex = "#FFE5EC",
                    authorOrPerson = "Diário Afetivo",
                    rotationDeg = 1.8f
                ),
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "bf_slot_2",
                    type = com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT,
                    title = "O Momento Mais Engraçado 😂",
                    actionPrompt = "Toque para colar aquela foto ou meme que sempre faz a gente gargalhar!",
                    rotationDeg = 2.0f
                ),
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "bf_note_2",
                    type = com.example.priv.data.model.ScrapbookItemType.NOTE,
                    title = "Trilha Sonora da Amizade 🎵",
                    uriOrContent = "Aquela música que quando toca no carro ou na playlist, todo mundo canta junto a plenos pulmões!",
                    noteColorHex = "#FFF9DB",
                    authorOrPerson = "Nossa Canção",
                    rotationDeg = -1.5f
                ),
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "bf_slot_3",
                    type = com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT,
                    title = "Nossa Próxima Aventura ✈️",
                    actionPrompt = "Toque para registrar a viagem, show ou rolê que vamos realizar juntos em breve!",
                    rotationDeg = 1.2f
                )
            )
        } else {
            // Template Inicial Acolhedor
            listOf(
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "starter_photo_slot",
                    type = com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT,
                    title = "Primeira Foto do Álbum 📸",
                    actionPrompt = "Toque para escolher uma foto marcante da galeria ou memórias!",
                    rotationDeg = -2.0f
                ),
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "starter_note",
                    type = com.example.priv.data.model.ScrapbookItemType.NOTE,
                    title = "Nosso Espaço de Afeto ✨",
                    uriOrContent = "Este mural foi criado para guardar nossos momentos mais preciosos, risadas e lembranças inesquecíveis! 💜",
                    noteColorHex = "#FFE5EC",
                    authorOrPerson = "GuardDay",
                    rotationDeg = 1.8f
                ),
                com.example.priv.data.model.ScrapbookPageItem(
                    id = "starter_note_slot",
                    type = com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT,
                    title = "Deixar um Bilhete do Coração 💌",
                    actionPrompt = "Toque aqui para escrever um recado ou frase marcante!",
                    rotationDeg = 1.5f
                )
            )
        }
        mutableStateOf(initialList)
    }

    // Controle de preenchimento interativo de slots
    var targetSlotItemForFill by remember { mutableStateOf<com.example.priv.data.model.ScrapbookPageItem?>(null) }

    // Modais e seletores interativos
    var showPhotoSourceChooser by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var noteDialogInitialTitle by remember { mutableStateOf("") }
    var showAppPhotosDialog by remember { mutableStateOf(false) }
    var showAddSlotChooser by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<com.example.priv.data.model.ScrapbookPageItem?>(null) }

    // Seletor de fotos da galeria com cópia segura permanente
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val updated = itemsList.toMutableList()
            val rotations = listOf(-2.5f, 2.0f, -1.8f, 2.8f, -2.0f, 1.5f)

            // Se o usuário tocou em um cartão de espaço temático para preencher com foto:
            val slotToReplace = targetSlotItemForFill
            if (slotToReplace != null && slotToReplace.type == com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT) {
                val slotIndex = updated.indexOfFirst { it.id == slotToReplace.id }
                val firstUri = uris.first()
                val localSavedPath = copyUriToScrapbookStorage(context, firstUri)
                val newPhotoItem = com.example.priv.data.model.ScrapbookPageItem(
                    id = slotToReplace.id,
                    type = com.example.priv.data.model.ScrapbookItemType.PHOTO,
                    uriOrContent = localSavedPath,
                    title = slotToReplace.title.replace(Regex("[📸😂✈️🏖️]"), "").trim().ifBlank { "Foto Especial" },
                    rotationDeg = slotToReplace.rotationDeg,
                    createdAt = System.currentTimeMillis()
                )
                if (slotIndex >= 0) {
                    updated[slotIndex] = newPhotoItem
                } else {
                    updated.add(newPhotoItem)
                }

                // Adiciona fotos extras selecionadas logo abaixo
                uris.drop(1).forEachIndexed { idx, remainingUri ->
                    val path = copyUriToScrapbookStorage(context, remainingUri)
                    updated.add(
                        com.example.priv.data.model.ScrapbookPageItem(
                            type = com.example.priv.data.model.ScrapbookItemType.PHOTO,
                            uriOrContent = path,
                            title = "Foto Adicionada",
                            rotationDeg = rotations[(updated.size + idx) % rotations.size],
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            } else {
                // Adição direta ao final do mural
                uris.forEachIndexed { index, uri ->
                    val localPath = copyUriToScrapbookStorage(context, uri)
                    updated.add(
                        com.example.priv.data.model.ScrapbookPageItem(
                            type = com.example.priv.data.model.ScrapbookItemType.PHOTO,
                            uriOrContent = localPath,
                            title = "Foto do Álbum",
                            rotationDeg = rotations[(updated.size + index) % rotations.size],
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            targetSlotItemForFill = null
            itemsList = updated
            onSaveItems(updated)
            android.widget.Toast.makeText(context, "${uris.size} foto(s) guardada(s) no mural! ✨", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            targetSlotItemForFill = null
        }
    }

    // Cores de fundo aconchegantes e confortáveis para leitura
    val backgroundBrush = remember(scrapbook.backgroundPattern) {
        when (scrapbook.backgroundPattern) {
            "DARK_NEBULA" -> Brush.verticalGradient(listOf(Color(0xFF0F0C1E), Color(0xFF1B162E)))
            "PASTEL_CREAM" -> Brush.verticalGradient(listOf(Color(0xFFFFFDF5), Color(0xFFFBF4E2)))
            "SOFT_LAVENDER" -> Brush.verticalGradient(listOf(Color(0xFF161226), Color(0xFF231C38)))
            else -> Brush.verticalGradient(listOf(Color(0xFF141220), Color(0xFF0D0B17)))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Área do Álbum com Zoom, Pan e Rolagem Vertical Suave
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.75f, 3.0f)
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-500f, 500f),
                            y = (offset.y + pan.y).coerceIn(-1000f, 1000f)
                        )
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 84.dp, bottom = 130.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabeçalho acolhedor do Álbum
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 520.dp)
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${scrapbook.coverEmojiOrIcon} ${scrapbook.title}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        if (scrapbook.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scrapbook.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Barra de figurinhas decorativas coladas no topo
                        val stickers = remember(scrapbook.stickersJson) {
                            try {
                                val arr = org.json.JSONArray(scrapbook.stickersJson)
                                (0 until arr.length()).map { arr.getString(it) }
                            } catch (e: Exception) {
                                listOf("💜", "✨", "📸", "👯", "⭐")
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            stickers.forEach { st ->
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(st, fontSize = 18.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Lista de Itens do Mural: Fotos Polaroid, Bilhetes e Espaços Interativos
                items(itemsList, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (item.type) {
                            com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT -> {
                                // Espaço Temático Interativo: Toque para Preencher na Hora!
                                ActionSlotCard(
                                    item = item,
                                    onClick = {
                                        targetSlotItemForFill = item
                                        if (item.actionPrompt.contains("foto", ignoreCase = true) || item.title.contains("Foto", ignoreCase = true)) {
                                            showPhotoSourceChooser = true
                                        } else {
                                            noteDialogInitialTitle = item.title.replace(Regex("[💌\\uD83D\\uDC9C]"), "").trim()
                                            showAddNoteDialog = true
                                        }
                                    }
                                )
                            }
                            com.example.priv.data.model.ScrapbookItemType.PHOTO -> {
                                InteractivePolaroidCard(
                                    item = item,
                                    onClick = { selectedItemForDetail = item }
                                )
                            }
                            com.example.priv.data.model.ScrapbookItemType.NOTE -> {
                                InteractiveNoteCard(
                                    item = item,
                                    onClick = { selectedItemForDetail = item }
                                )
                            }
                            com.example.priv.data.model.ScrapbookItemType.STICKER -> {
                                Text(
                                    text = item.uriOrContent,
                                    fontSize = 46.sp,
                                    modifier = Modifier
                                        .rotate(item.rotationDeg)
                                        .clickable { selectedItemForDetail = item }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Barra Superior Flutuante: Controles de Zoom e Fechar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar Álbum", tint = MaterialTheme.colorScheme.onSurface)
                }

                Text(
                    text = scrapbook.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )

                IconButton(
                    onClick = { scale = max(0.75f, scale - 0.2f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Diminuir zoom", modifier = Modifier.size(18.dp))
                }

                Text(
                    text = "${(scale * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { scale = min(2.5f, scale + 0.2f) },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Aumentar zoom", modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset.Zero
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Encaixar na tela", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Doca Inferior Flutuante com Ações Rápidas de Criação (acessível ao polegar!)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 12.dp, end = 12.dp)
                .shadow(12.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Adicionar Foto (+ Foto)
                FilledTonalButton(
                    onClick = {
                        targetSlotItemForFill = null
                        showPhotoSourceChooser = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_scrapbook_add_gallery_photo")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Adicionar Bilhete Afetuoso (+ Bilhete)
                FilledTonalButton(
                    onClick = {
                        targetSlotItemForFill = null
                        noteDialogInitialTitle = ""
                        showAddNoteDialog = true
                    },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_scrapbook_add_note")
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bilhete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 3. Adicionar Adesivo
                IconButton(
                    onClick = { showStickerSheet = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Mood, contentDescription = "Colar Adesivo", tint = MaterialTheme.colorScheme.primary)
                }

                // 4. Adicionar Espaço Temático (+ Espaço)
                IconButton(
                    onClick = { showAddSlotChooser = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = "Adicionar Espaço Temático", tint = MaterialTheme.colorScheme.primary)
                }

                // 5. Trocar Fundo e Estilo
                IconButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Trocar Fundo do Álbum", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    // DIÁLOGO SELETOR DE ORIGEM DA FOTO: Galeria ou Memórias do App
    if (showPhotoSourceChooser) {
        AlertDialog(
            onDismissRequest = {
                showPhotoSourceChooser = false
            },
            title = {
                Text(
                    text = if (targetSlotItemForFill != null) "Adicionar Foto: ${targetSlotItemForFill!!.title}" else "Adicionar Foto ao Mural",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "De onde você deseja selecionar a sua foto?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceChooser = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Galeria do Celular", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Escolher fotos da câmera e do aparelho", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoSourceChooser = false
                                showAppPhotosDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Collections, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Fotos do GuardDay", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Usar fotos já anexadas nas suas memórias", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoSourceChooser = false }) { Text("Cancelar") }
            }
        )
    }

    // DIÁLOGO 1: Escrever Bilhetinho Afetuoso
    if (showAddNoteDialog) {
        var noteTitle by remember { mutableStateOf(noteDialogInitialTitle) }
        var noteContent by remember { mutableStateOf("") }
        var noteAuthor by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#FFE5EC") }

        AlertDialog(
            onDismissRequest = {
                showAddNoteDialog = false
                targetSlotItemForFill = null
            },
            title = { Text(if (targetSlotItemForFill != null) "Preencher Bilhete" else "Escrever um Bilhete", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        label = { Text("Título ou Assunto") },
                        placeholder = { Text("Ex: Segredo nosso, Frase marcante") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { noteContent = it },
                        label = { Text("Mensagem / Lembrança") },
                        placeholder = { Text("Escreva uma frase, memória ou recado carinhoso...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = noteAuthor,
                        onValueChange = { noteAuthor = it },
                        label = { Text("Quem escreveu? (Opcional)") },
                        placeholder = { Text("Ex: Lucas, Vovó Helena, Amigos") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Cor do Papel:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "#FFE5EC" to "Rosa",
                            "#FFF9DB" to "Creme",
                            "#E3FAFC" to "Céu",
                            "#EBFBEE" to "Menta",
                            "#F3F0FF" to "Lavanda"
                        ).forEach { (colorHex, _) ->
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColorHex == colorHex) 3.dp else 1.dp,
                                        color = if (selectedColorHex == colorHex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = colorHex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteContent.isNotBlank()) {
                            val updated = itemsList.toMutableList()
                            val slotToReplace = targetSlotItemForFill
                            if (slotToReplace != null && slotToReplace.type == com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT) {
                                val slotIndex = updated.indexOfFirst { it.id == slotToReplace.id }
                                val newNoteItem = com.example.priv.data.model.ScrapbookPageItem(
                                    id = slotToReplace.id,
                                    type = com.example.priv.data.model.ScrapbookItemType.NOTE,
                                    title = noteTitle.ifBlank { slotToReplace.title },
                                    uriOrContent = noteContent,
                                    noteColorHex = selectedColorHex,
                                    authorOrPerson = noteAuthor,
                                    rotationDeg = slotToReplace.rotationDeg,
                                    createdAt = System.currentTimeMillis()
                                )
                                if (slotIndex >= 0) {
                                    updated[slotIndex] = newNoteItem
                                } else {
                                    updated.add(newNoteItem)
                                }
                            } else {
                                updated.add(
                                    com.example.priv.data.model.ScrapbookPageItem(
                                        type = com.example.priv.data.model.ScrapbookItemType.NOTE,
                                        title = noteTitle.ifBlank { "Lembrança Afetiva" },
                                        uriOrContent = noteContent,
                                        noteColorHex = selectedColorHex,
                                        authorOrPerson = noteAuthor,
                                        rotationDeg = (listOf(-2f, 2.5f, -3f, 1.8f)[updated.size % 4]),
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                            targetSlotItemForFill = null
                            itemsList = updated
                            onSaveItems(updated)
                            showAddNoteDialog = false
                            android.widget.Toast.makeText(context, "Bilhete colado no mural! ✨", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = noteContent.isNotBlank()
                ) {
                    Text("Colar no Mural ✨")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddNoteDialog = false
                    targetSlotItemForFill = null
                }) { Text("Cancelar") }
            }
        )
    }

    // DIÁLOGO 2: Selecionar Fotos das Memórias Existentes do App
    if (showAppPhotosDialog) {
        val appPhotos = remember(memories) {
            memories.flatMap { mem ->
                mem.attachments.filter { it.type == "IMAGE" }.map { it to mem.memory.title }
            }
        }

        AlertDialog(
            onDismissRequest = {
                showAppPhotosDialog = false
                targetSlotItemForFill = null
            },
            title = { Text("Fotos das Memórias do GuardDay", fontWeight = FontWeight.Bold) },
            text = {
                if (appPhotos.isEmpty()) {
                    Text("Nenhuma foto encontrada nas suas memórias registradas ainda. Escolha a opção Galeria do Celular para selecionar fotos do seu aparelho!", fontSize = 13.sp)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(appPhotos) { (attachment, memTitle) ->
                            val photoPath = attachment.localPath.ifBlank { attachment.uri }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clickable {
                                        val updated = itemsList.toMutableList()
                                        val slotToReplace = targetSlotItemForFill
                                        if (slotToReplace != null && slotToReplace.type == com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT) {
                                            val slotIndex = updated.indexOfFirst { it.id == slotToReplace.id }
                                            val newPhotoItem = com.example.priv.data.model.ScrapbookPageItem(
                                                id = slotToReplace.id,
                                                type = com.example.priv.data.model.ScrapbookItemType.PHOTO,
                                                uriOrContent = photoPath,
                                                title = slotToReplace.title.replace(Regex("[📸😂✈️🏖️]"), "").trim().ifBlank { memTitle },
                                                rotationDeg = slotToReplace.rotationDeg,
                                                createdAt = System.currentTimeMillis()
                                            )
                                            if (slotIndex >= 0) {
                                                updated[slotIndex] = newPhotoItem
                                            } else {
                                                updated.add(newPhotoItem)
                                            }
                                        } else {
                                            updated.add(
                                                com.example.priv.data.model.ScrapbookPageItem(
                                                    type = com.example.priv.data.model.ScrapbookItemType.PHOTO,
                                                    uriOrContent = photoPath,
                                                    title = memTitle,
                                                    rotationDeg = (listOf(-2.5f, 2f, -1.8f, 2.5f)[updated.size % 4]),
                                                    createdAt = System.currentTimeMillis()
                                                )
                                            )
                                        }
                                        targetSlotItemForFill = null
                                        itemsList = updated
                                        onSaveItems(updated)
                                        showAppPhotosDialog = false
                                        android.widget.Toast.makeText(context, "Foto adicionada ao mural! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(File(photoPath).takeIf { it.exists() } ?: photoPath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAppPhotosDialog = false
                    targetSlotItemForFill = null
                }) { Text("Fechar") }
            }
        )
    }

    // DIÁLOGO SELETOR DE NOVO ESPAÇO TEMÁTICO (+ Espaço)
    if (showAddSlotChooser) {
        val suggestedSlots = listOf(
            Triple("Foto com Amigo(a) 📸", "Toque para colar uma foto de amizade marcante!", -2.5f),
            Triple("O Momento Mais Engraçado 😂", "Toque para colar aquela foto ou meme inesquecível!", 2.0f),
            Triple("Nossa Próxima Aventura ✈️", "Toque para planejar a próxima viagem ou encontro!", 1.5f),
            Triple("Trilha Sonora da Amizade 🎵", "Toque para escrever a música que marca vocês!", -1.8f),
            Triple("Lembrança Inesquecível 🌟", "Toque para deixar uma recordação marcante!", 2.2f)
        )

        AlertDialog(
            onDismissRequest = { showAddSlotChooser = false },
            title = { Text("Adicionar Espaço Temático", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Escolha um tema para convidar a preencher no mural:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    suggestedSlots.forEach { (slotTitle, prompt, rot) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = itemsList.toMutableList()
                                    updated.add(
                                        com.example.priv.data.model.ScrapbookPageItem(
                                            type = com.example.priv.data.model.ScrapbookItemType.ACTION_SLOT,
                                            title = slotTitle,
                                            actionPrompt = prompt,
                                            rotationDeg = rot,
                                            createdAt = System.currentTimeMillis()
                                        )
                                    )
                                    itemsList = updated
                                    onSaveItems(updated)
                                    showAddSlotChooser = false
                                    android.widget.Toast.makeText(context, "Espaço adicionado ao mural!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(slotTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(prompt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddSlotChooser = false }) { Text("Fechar") }
            }
        )
    }

    // DIÁLOGO 3: Seletor de Adesivos e Selos Emocionais
    if (showStickerSheet) {
        AlertDialog(
            onDismissRequest = { showStickerSheet = false },
            title = { Text("Escolher Adesivo Afetivo", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Toque em um adesivo para colar no seu álbum:", fontSize = 13.sp)
                    val availableStickers = listOf(
                        "💖", "👯", "🫂", "✨", "⭐", "🌸", "💌", "☕",
                        "🎵", "📸", "🍕", "🏖️", "🎉", "🔥", "🎂", "🌻"
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableStickers) { sticker ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clickable {
                                        val updated = itemsList.toMutableList()
                                        updated.add(
                                            com.example.priv.data.model.ScrapbookPageItem(
                                                type = com.example.priv.data.model.ScrapbookItemType.STICKER,
                                                uriOrContent = sticker,
                                                rotationDeg = (listOf(-5f, 4f, -3f, 6f)[updated.size % 4]),
                                                createdAt = System.currentTimeMillis()
                                            )
                                        )
                                        itemsList = updated
                                        onSaveItems(updated)
                                        showStickerSheet = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(sticker, fontSize = 26.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStickerSheet = false }) { Text("Fechar") }
            }
        )
    }

    // DIÁLOGO 4: Trocar Fundo e Estilo do Álbum
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Estilo do Fundo", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        "PAPER_GRID" to "Papel Quadriculado Clássico 📄",
                        "SOFT_LAVENDER" to "Noite Suave & Lavanda 💜",
                        "PASTEL_CREAM" to "Creme Quente & Afeto ☕",
                        "DARK_NEBULA" to "Céu Noturno Profundo ✨"
                    ).forEach { (patternKey, label) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (scrapbook.backgroundPattern == patternKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateTheme(patternKey)
                                    showThemeDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // DIÁLOGO 5: Detalhe do Item Selecionado (Ver Grande, Editar Legenda, Girar ou Remover)
    if (selectedItemForDetail != null) {
        val item = selectedItemForDetail!!
        var editedCaption by remember(item.id) { mutableStateOf(item.title) }

        AlertDialog(
            onDismissRequest = { selectedItemForDetail = null },
            title = {
                Text(
                    text = when (item.type) {
                        com.example.priv.data.model.ScrapbookItemType.PHOTO -> "Detalhes da Foto"
                        com.example.priv.data.model.ScrapbookItemType.NOTE -> "Bilhete Afetivo"
                        else -> "Item do Mural"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (item.type == com.example.priv.data.model.ScrapbookItemType.PHOTO) {
                        val imgFile = remember(item.uriOrContent) { File(item.uriOrContent) }
                        val model = if (imgFile.exists()) imgFile else item.uriOrContent
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(model).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        OutlinedTextField(
                            value = editedCaption,
                            onValueChange = { editedCaption = it },
                            label = { Text("Legenda da Foto") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else if (item.type == com.example.priv.data.model.ScrapbookItemType.NOTE) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(android.graphics.Color.parseColor(item.noteColorHex)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (item.title.isNotBlank()) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2C243B))
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = item.uriOrContent,
                                    fontSize = 14.sp,
                                    color = Color(0xFF2C243B)
                                )
                                if (item.authorOrPerson.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("— ${item.authorOrPerson}", fontSize = 11.sp, color = Color(0xFF5A5266))
                                }
                            }
                        }
                    }

                    // Ações Rápidas: Girar levemente
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextRot = if (item.rotationDeg < 0) 2.5f else -2.5f
                                val updated = itemsList.map { if (it.id == item.id) it.copy(rotationDeg = nextRot) else it }
                                itemsList = updated
                                onSaveItems(updated)
                                selectedItemForDetail = selectedItemForDetail?.copy(rotationDeg = nextRot)
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Girar")
                        }

                        if (item.type == com.example.priv.data.model.ScrapbookItemType.PHOTO) {
                            Button(
                                onClick = {
                                    val updated = itemsList.map { if (it.id == item.id) it.copy(title = editedCaption) else it }
                                    itemsList = updated
                                    onSaveItems(updated)
                                    selectedItemForDetail = null
                                    android.widget.Toast.makeText(context, "Legenda atualizada! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Salvar Legenda")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = itemsList.filter { it.id != item.id }
                        itemsList = updated
                        onSaveItems(updated)
                        selectedItemForDetail = null
                        android.widget.Toast.makeText(context, "Item removido do álbum", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Remover do Álbum", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForDetail = null }) { Text("Fechar") }
            }
        )
    }
}

/**
 * Cartão de Espaço Temático Interativo (convida o usuário a agir com toque direto!)
 */
@Composable
private fun ActionSlotCard(
    item: com.example.priv.data.model.ScrapbookPageItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .rotate(item.rotationDeg)
            .clickable { onClick() }
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.actionPrompt.contains("foto", ignoreCase = true) || item.title.contains("Foto", ignoreCase = true)) Icons.Default.AddPhotoAlternate else Icons.Default.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.actionPrompt,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (item.actionPrompt.contains("foto", ignoreCase = true) || item.title.contains("Foto", ignoreCase = true)) Icons.Default.AddAPhoto else Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Toque para Preencher ✨",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Cartão Polaroid Estético com Fita Adesiva (Washi Tape) e Interação de Toque
 */
@Composable
private fun InteractivePolaroidCard(
    item: com.example.priv.data.model.ScrapbookPageItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .rotate(item.rotationDeg)
            .clickable { onClick() }
            .shadow(10.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFDF5) // Papel marfim tipo polaroid clássica
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Fita Adesiva Decorativa (Washi Tape) no topo
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFE8D5B5).copy(alpha = 0.85f))
            )

            Spacer(modifier = Modifier.height(10.dp))

            val imgFile = remember(item.uriOrContent) { File(item.uriOrContent) }
            val imgModel = if (imgFile.exists()) imgFile else item.uriOrContent

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imgModel)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFEFEF))
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title.ifBlank { "Lembrança GuardDay" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C243B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Toque para ver em detalhes ou editar legenda 🔍",
                fontSize = 10.sp,
                color = Color(0xFF7A7289),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Cartão de Bilhete Afetuoso (Post-it / Recadinho)
 */
@Composable
private fun InteractiveNoteCard(
    item: com.example.priv.data.model.ScrapbookPageItem,
    onClick: () -> Unit
) {
    val cardColor = remember(item.noteColorHex) {
        try {
            Color(android.graphics.Color.parseColor(item.noteColorHex))
        } catch (e: Exception) {
            Color(0xFFFFF9DB)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .rotate(item.rotationDeg)
            .clickable { onClick() }
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Alfinete / Pin
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE63946),
                    modifier = Modifier.size(10.dp)
                ) {}

                Text(
                    text = "💌 Bilhete",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5A5266)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (item.title.isNotBlank()) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C243B)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = "\"${item.uriOrContent}\"",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFF3B334A)
            )

            if (item.authorOrPerson.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "— ${item.authorOrPerson}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF5A5266)
                )
            }
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
 * Galeria de Murais e Álbuns de Memórias
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
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhum Álbum de Memórias criado ainda",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Crie murais de fotos, colagens de lembranças e bilhetes carinhosos para guardar momentos com quem você ama!",
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
                    Text("Criar Primeiro Álbum")
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
                            text = "Mural Afetivo • Toque para folhear e personalizar",
                            fontSize = 11.sp,
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
 * Diálogo para Criação de Novo Álbum de Lembranças
 */
@Composable
fun CreateScrapbookDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, styleTheme: String, emoji: String, stickers: List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var styleTheme by remember { mutableStateOf("VINTAGE_POLAROID") }
    var selectedEmoji by remember { mutableStateOf("✨") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Álbum de Memórias", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Nome do Álbum") },
                    placeholder = { Text("Ex: Nossas Viagens de Férias, Diário dos Amigos") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Dedicatória ou Frase de Abertura") },
                    placeholder = { Text("Ex: Para guardar nossas maiores risadas e momentos juntos") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Ícone de Capa:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("✨", "💖", "👯", "🌊", "☕", "🍕", "🌻", "⭐").forEach { em ->
                        FilterChip(
                            selected = selectedEmoji == em,
                            onClick = { selectedEmoji = em },
                            label = { Text(em, fontSize = 16.sp) }
                        )
                    }
                }

                Text("Estilo Visual do Mural:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "VINTAGE_POLAROID" to "Vintage & Afeto",
                        "CLASSIC_PAPER" to "Papel Clássico",
                        "SOFT_LAVENDER" to "Noite Suave"
                    ).forEach { (key, label) ->
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
                        onConfirm(title, desc, styleTheme, selectedEmoji, listOf("💖", "👯", "✨", "🎵", "⭐"))
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Criar Álbum ✨")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
