package com.example.priv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.data.entity.CollectionEntity
import com.example.priv.data.entity.MomentEntity
import com.example.priv.ui.components.ReusableMemoryTimeline
import com.example.priv.ui.viewmodel.MomentsAndCollectionsViewModel

@Composable
fun MomentsAndCollectionsScreen(
    viewModel: MomentsAndCollectionsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Momentos, 1: Coleções
    var selectedMomentId by remember { mutableStateOf<String?>(null) }
    var selectedCollectionId by remember { mutableStateOf<String?>(null) }

    var showCreateMomentDialog by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }

    val moments by viewModel.moments.collectAsState()
    val collections by viewModel.collections.collectAsState()

    val selectedMoment = moments.find { it.id == selectedMomentId }
    val selectedCollection = collections.find { it.id == selectedCollectionId }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    selectedMomentId = null
                    selectedCollectionId = null
                },
                text = { Text("Momentos & Eventos", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    selectedMomentId = null
                    selectedCollectionId = null
                },
                text = { Text("Coleções Especiais", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            if (selectedMomentId != null && selectedMoment != null) {
                // Moment Detail View
                MomentDetailView(
                    moment = selectedMoment,
                    viewModel = viewModel,
                    onBack = { selectedMomentId = null }
                )
            } else {
                // Moments List
                MomentsListView(
                    moments = moments,
                    onSelectMoment = { selectedMomentId = it },
                    onCreateMomentClick = { showCreateMomentDialog = true }
                )
            }
        } else {
            if (selectedCollectionId != null && selectedCollection != null) {
                // Collection Detail View
                CollectionDetailView(
                    collection = selectedCollection,
                    viewModel = viewModel,
                    onBack = { selectedCollectionId = null }
                )
            } else {
                // Collections List
                CollectionsListView(
                    collections = collections,
                    onSelectCollection = { selectedCollectionId = it },
                    onCreateCollectionClick = { showCreateCollectionDialog = true }
                )
            }
        }
    }

    if (showCreateMomentDialog) {
        CreateMomentDialog(
            onDismiss = { showCreateMomentDialog = false },
            onCreate = { title, desc, date, cat, colorHex ->
                viewModel.createMoment(title, desc, date, cat, colorHex)
                showCreateMomentDialog = false
            }
        )
    }

    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onCreate = { title, desc, iconName, colorHex ->
                viewModel.createCollection(title, desc, iconName, colorHex)
                showCreateCollectionDialog = false
            }
        )
    }
}

@Composable
fun MomentsListView(
    moments: List<MomentEntity>,
    onSelectMoment: (String) -> Unit,
    onCreateMomentClick: () -> Unit
) {
    Column(
        modifier = Modifier
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
                    text = "Momentos & Eventos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Aniversários, viagens, conquistas e histórias",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onCreateMomentClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_moment_btn")
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (moments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum momento cadastrado. Crie momentos como 'Aniversário 2026' ou 'Viagem pra Praia'.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(moments) { moment ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectMoment(moment.id) }
                            .testTag("moment_item_${moment.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            runCatching { Color(android.graphics.Color.parseColor(moment.colorHex)) }
                                                .getOrDefault(MaterialTheme.colorScheme.primary)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = Color.White)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = moment.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (moment.description.isNotBlank()) {
                                        Text(
                                            text = moment.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (moment.date.isNotBlank()) {
                                Text(
                                    text = moment.date,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionsListView(
    collections: List<CollectionEntity>,
    onSelectCollection: (String) -> Unit,
    onCreateCollectionClick: () -> Unit
) {
    Column(
        modifier = Modifier
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
                    text = "Coleções Especiais",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pastas e agrupamentos de memórias marcantes",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onCreateCollectionClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("add_collection_btn")
            ) {
                Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nova", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (collections.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma coleção criada ainda.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(collections) { col ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectCollection(col.id) }
                            .testTag("collection_item_${col.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        runCatching { Color(android.graphics.Color.parseColor(col.colorHex)) }
                                            .getOrDefault(MaterialTheme.colorScheme.secondary)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = col.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (col.description.isNotBlank()) {
                                    Text(
                                        text = col.description,
                                        fontSize = 12.sp,
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

@Composable
fun MomentDetailView(
    moment: MomentEntity,
    viewModel: MomentsAndCollectionsViewModel,
    onBack: () -> Unit
) {
    val memories by viewModel.getMemoriesForMoment(moment.id).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Voltar para Momentos")
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = moment.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (moment.description.isNotBlank()) {
                    Text(
                        text = moment.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                if (moment.date.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Data: ${moment.date}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ReusableMemoryTimeline(
            memories = memories,
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onMoveToTrash = { viewModel.moveToTrash(it) },
            emptyMessage = "Nenhuma memória associada ao momento '${moment.title}' ainda."
        )
    }
}

@Composable
fun CollectionDetailView(
    collection: CollectionEntity,
    viewModel: MomentsAndCollectionsViewModel,
    onBack: () -> Unit
) {
    val memories by viewModel.getMemoriesForCollection(collection.id).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Voltar para Coleções")
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = collection.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (collection.description.isNotBlank()) {
                    Text(
                        text = collection.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ReusableMemoryTimeline(
            memories = memories,
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onMoveToTrash = { viewModel.moveToTrash(it) },
            emptyMessage = "Nenhuma memória nesta coleção ainda."
        )
    }
}

@Composable
fun CreateMomentDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String, date: String, cat: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Momento / Evento", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título do evento (ex: Viagem Ubatuba)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descrição curta") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (ex: Jan 2026)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onCreate(title, desc, date, "Geral", "#FF4B72") }, enabled = title.isNotBlank()) {
                Text("Criar Momento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String, iconName: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Coleção Especial", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título da coleção (ex: Áudios Épicos)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onCreate(title, desc, "Favorite", "#8E3BEE") }, enabled = title.isNotBlank()) {
                Text("Criar Coleção")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
