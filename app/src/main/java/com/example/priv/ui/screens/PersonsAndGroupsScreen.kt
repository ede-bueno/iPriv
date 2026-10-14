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
import com.example.priv.data.entity.GroupEntity
import com.example.priv.data.entity.PersonEntity
import com.example.priv.data.model.GroupWithDetails
import com.example.priv.ui.components.ReusableMemoryTimeline
import com.example.priv.ui.viewmodel.GroupsViewModel
import com.example.priv.ui.viewmodel.PersonViewModel

@Composable
fun PersonsAndGroupsScreen(
    personViewModel: PersonViewModel,
    groupsViewModel: GroupsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Pessoas, 1: Grupos
    var selectedGroupId by remember { mutableStateOf<String?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val selectedGroupWithDetails = groupsViewModel.groups.collectAsState().value.find { it.group.id == selectedGroupId }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar Header
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    selectedGroupId = null
                },
                text = { Text("Pessoas", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Grupos", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            PersonsScreen(
                viewModel = personViewModel,
                onSelectPerson = { personViewModel.selectPerson(it) }
            )
        } else {
            // Grupos Tab
            if (selectedGroupId != null && selectedGroupWithDetails != null) {
                GroupDetailView(
                    groupWithDetails = selectedGroupWithDetails,
                    groupsViewModel = groupsViewModel,
                    onBack = { selectedGroupId = null }
                )
            } else {
                GroupsListView(
                    groupsViewModel = groupsViewModel,
                    onSelectGroup = { selectedGroupId = it },
                    onCreateGroupClick = { showCreateGroupDialog = true }
                )
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            persons = groupsViewModel.persons.collectAsState().value,
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, colorHex, memberIds ->
                groupsViewModel.createGroup(name, desc, colorHex, "Group", memberIds)
                showCreateGroupDialog = false
            }
        )
    }
}

@Composable
fun GroupsListView(
    groupsViewModel: GroupsViewModel,
    onSelectGroup: (String) -> Unit,
    onCreateGroupClick: () -> Unit
) {
    val groups by groupsViewModel.groups.collectAsState()

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
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = "Grupos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Memórias da galera, família e turmas",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onCreateGroupClick,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_group_btn")
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo Grupo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum grupo criado ainda. Crie um grupo como 'Família' ou 'Galera do 3º Ano'.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(groups) { groupWithDetails ->
                    GroupCardItem(
                        groupWithDetails = groupWithDetails,
                        onClick = { onSelectGroup(groupWithDetails.group.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupCardItem(
    groupWithDetails: GroupWithDetails,
    onClick: () -> Unit
) {
    val group = groupWithDetails.group
    val members = groupWithDetails.members

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("group_item_${group.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                runCatching { Color(android.graphics.Color.parseColor(group.colorHex)) }
                                    .getOrDefault(MaterialTheme.colorScheme.primary)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = group.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (group.description.isNotBlank()) {
                            Text(
                                text = group.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Member Avatars Stack
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                members.take(4).forEach { member ->
                    Surface(
                        shape = CircleShape,
                        color = runCatching { Color(android.graphics.Color.parseColor(member.colorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                if (members.size > 4) {
                    Text(
                        text = "+${members.size - 4}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${members.size} membros",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GroupDetailView(
    groupWithDetails: GroupWithDetails,
    groupsViewModel: GroupsViewModel,
    onBack: () -> Unit
) {
    val group = groupWithDetails.group
    val members = groupWithDetails.members
    val memories by groupsViewModel.getMemoriesForGroup(group.id).collectAsState(initial = emptyList())

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Voltar para Grupos")
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir Grupo", tint = MaterialTheme.colorScheme.error)
            }
        }

        // Header Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                runCatching { Color(android.graphics.Color.parseColor(group.colorHex)) }
                                    .getOrDefault(MaterialTheme.colorScheme.primary)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = group.name,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = group.description.ifBlank { "Grupo de memórias" },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Membros do Grupo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { m ->
                        AssistChip(
                            onClick = {},
                            label = { Text(m.name, fontSize = 11.sp) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(runCatching { Color(android.graphics.Color.parseColor(m.colorHex)) }.getOrDefault(Color.Magenta))
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Timeline do Grupo",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        ReusableMemoryTimeline(
            memories = memories,
            onToggleFavorite = { groupsViewModel.toggleFavorite(it) },
            onMoveToTrash = { groupsViewModel.moveToTrash(it) },
            emptyMessage = "Nenhuma memória associada ao grupo '${group.name}' ainda."
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Grupo?") },
            text = { Text("A exclusão do grupo NÃO apagará as pessoas nem as memórias associadas. Apenas desvinculará o grupo.") },
            confirmButton = {
                Button(
                    onClick = {
                        groupsViewModel.deleteGroup(group.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir Grupo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun CreateGroupDialog(
    persons: List<PersonEntity>,
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, colorHex: String, memberIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf("#8E3BEE") }
    val selectedMemberIds = remember { mutableStateListOf<String>() }

    val colors = listOf("#8E3BEE", "#FF4B72", "#00F5D4", "#FFB703", "#3A86FF")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Criar Novo Grupo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do grupo (ex: Família, Turma 3º Ano)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descrição curta") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cor do grupo:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(c)))
                                .clickable { colorHex = c }
                        )
                    }
                }

                Text("Selecione os membros:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    persons.forEach { p ->
                        val isSel = selectedMemberIds.contains(p.id)
                        FilterChip(
                            selected = isSel,
                            onClick = { if (isSel) selectedMemberIds.remove(p.id) else selectedMemberIds.add(p.id) },
                            label = { Text(p.name, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, desc, colorHex, selectedMemberIds.toList()) },
                enabled = name.isNotBlank()
            ) {
                Text("Criar Grupo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
