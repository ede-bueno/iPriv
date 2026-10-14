package com.example.priv.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.priv.data.contacts.PhoneContactItem
import com.example.priv.data.viewmodel.ProfileViewModel
import com.example.priv.ui.preferences.UserPreferencesMenuSheet
import com.example.priv.ui.audio.AudioRecordingBottomSheet
import com.example.priv.ui.audio.AudioMemoriesModalSheet

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showContactsDialog by remember { mutableStateOf(false) }
    var showPreferencesMenu by remember { mutableStateOf(false) }
    var showAudioRecorderModal by remember { mutableStateOf(false) }
    var showAudioMemoriesVault by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf("") }
    var editPhotoUrl by remember { mutableStateOf("") }

    var exportedBackupContent by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Toast feedback
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    // Gallery photo picker launcher for profile picture
    val profilePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            editPhotoUrl = uri.toString()
        }
    }

    // SAF launcher for manual JSON export
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            exportedBackupContent?.let { json ->
                viewModel.writeBackupToUri(uri, json)
            }
        }
    }

    // SAF launcher for Full Backup ZIP export
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            viewModel.exportFullBackupZip(uri)
        }
    }

    // SAF launcher for Restore Backup
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = uri
        }
    }

    // Contacts Permission Request launcher
    val requestContactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadDeviceContacts()
            showContactsDialog = true
        } else {
            Toast.makeText(context, "Permissão de contatos negada. O Priv continua funcionando normalmente.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Perfil & Espaço Privado",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Identidade do proprietário e dados do seu GuardDay",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalIconButton(
                onClick = { showPreferencesMenu = true },
                modifier = Modifier.testTag("btn_open_preferences_menu")
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Menu de Preferências",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // User Identity Header Card (Proprietário da Conta)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                if (!uiState.session?.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = uiState.session?.photoUrl,
                                        contentDescription = "Foto do Usuário",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.session?.displayName?.ifBlank { "Proprietário do Priv" } ?: "Proprietário do Priv",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = uiState.session?.email ?: "usuario@priv.app",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Espaço Ativo: ${uiState.session?.personalSpaceId ?: "space_personal_usr"}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    editName = uiState.session?.displayName.orEmpty()
                                    editPhotoUrl = uiState.session?.photoUrl.orEmpty()
                                    showEditProfileDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Editar Perfil", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sair", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Menu de Preferências do Usuário (Card de Destaque)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPreferencesMenu = true }
                        .testTag("card_open_user_preferences")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Menu de Preferências",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Editar tema (claro/escuro), perfil, áudio, segurança e notificações",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Cofre de Memórias de Áudio & Transcrições (Banco Room)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAudioMemoriesVault = true }
                        .testTag("card_open_audio_memories_vault")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Memórias de Áudio & Transcrições",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Banco local Room com player, transcrições e metadados",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Google Login Integration Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Autenticação Google Credential Manager", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Conecte sua conta Google para sincronização segura no Supabase e restauração automática de sessão.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.session?.authProvider == "Google") {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Conectado com Google Auth (${uiState.session?.email})", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = { viewModel.signInWithGoogle() },
                                enabled = !uiState.isLoading,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_login_btn")
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Conectando com Google...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Entrar / Vincular com Google", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Storage & Local/Remote Statistics Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Estatísticas do Espaço", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Memórias", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${uiState.memoryCount}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pessoas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${uiState.personCount}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Local", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${uiState.localStorageMb} MB", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }

            // Backup Manual / Google Drive Integration Foundation
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup & Restauração de Dados", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Gere um pacote de backup com manifesto, checksum e metadados completos, ou exporte um ZIP com todas as mídias. Você pode salvar no armazenamento local, Google Drive ou outro provedor via SAF.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportBackupToJson { json ->
                                        exportedBackupContent = json
                                        createDocumentLauncher.launch("priv_backup_metadata_${System.currentTimeMillis()}.json")
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar Metadados (JSON)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    createZipLauncher.launch("priv_backup_full_${System.currentTimeMillis()}.zip")
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar Backup Completo (ZIP + Mídias)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar / Importar Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Contatos do Aparelho (Importação Opcional e Seletiva)
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importação de Contatos (Opcional)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Importe contatos individualmente da sua agenda para preencher suas Pessoas no Priv. O aplicativo NÃO envia sua agenda para o servidor.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (uiState.contactsPermissionGranted) {
                                    viewModel.loadDeviceContacts()
                                    showContactsDialog = true
                                } else {
                                    requestContactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Escolher Contatos para Importar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Security & Privacy Terms Card with PIN & Biometric Protection
            item {
                var isPinActive by remember { mutableStateOf(com.example.priv.data.security.SecurityLockManager.isPinEnabled(context)) }
                var showSetPinDialog by remember { mutableStateOf(false) }
                var newPinInput by remember { mutableStateOf("") }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Proteção por Biometria / PIN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Exija digital ou PIN de 4 dígitos ao abrir o Priv para garantir privacidade total dos seus áudios e memórias.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPinActive) "Bloqueio por PIN/Digital ATIVADO" else "Bloqueio DESATIVADO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPinActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Switch(
                                checked = isPinActive,
                                onCheckedChange = { enable ->
                                    if (enable) {
                                        showSetPinDialog = true
                                    } else {
                                        com.example.priv.data.security.SecurityLockManager.disableSecurity(context)
                                        isPinActive = false
                                        Toast.makeText(context, "Proteção por PIN desativada.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }

                        if (isPinActive) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    com.example.priv.data.security.SecurityLockManager.lockNow()
                                    Toast.makeText(context, "Priv bloqueado agora! 🔒", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bloquear Priv Agora", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { showPrivacyDialog = true }
                        ) {
                            Text("Ver Termos de Privacidade e Proteção", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showSetPinDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showSetPinDialog = false
                            newPinInput = ""
                        },
                        title = { Text("Definir PIN de 4 Dígitos", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Digite uma senha numérica de 4 dígitos para proteger o Priv:", fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = newPinInput,
                                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinInput = it },
                                    label = { Text("PIN (4 números)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (newPinInput.length == 4) {
                                        com.example.priv.data.security.SecurityLockManager.setPin(context, newPinInput, enableBiometrics = true)
                                        isPinActive = true
                                        showSetPinDialog = false
                                        newPinInput = ""
                                        Toast.makeText(context, "PIN de segurança configurado com sucesso! 🔒", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Digite exatamente 4 números.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = newPinInput.length == 4
                            ) {
                                Text("Ativar Proteção")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showSetPinDialog = false
                                    newPinInput = ""
                                }
                            ) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }

            // Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "GuardDay v1.0.0-beta.4 (Build 5)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Guarda as vozes e momentos que não podem sumir.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Editar Perfil do Proprietário", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Live Avatar Preview
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = editPhotoUrl,
                                contentDescription = "Prévia da Foto",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Button to pick photo from device gallery
                    Button(
                        onClick = { profilePhotoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escolher Foto da Galeria", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nome de Exibição") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (editPhotoUrl.isNotBlank()) {
                        TextButton(onClick = { editPhotoUrl = "" }) {
                            Text("Remover Foto do Perfil", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfileNameAndPhoto(editName, editPhotoUrl.ifBlank { null })
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Contacts Import Selection Dialog
    if (showContactsDialog) {
        AlertDialog(
            onDismissRequest = { showContactsDialog = false },
            title = { Text("Importar Contato para 'Pessoas'", fontWeight = FontWeight.Bold) },
            text = {
                if (uiState.deviceContacts.isEmpty()) {
                    Text("Nenhum contato encontrado na agenda do aparelho.", fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.deviceContacts) { contact ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.importSelectiveContact(contact)
                                        showContactsDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Toque para importar como Pessoa", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactsDialog = false }) { Text("Fechar") }
            }
        )
    }

    // Privacy Terms Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacidade & Proteção de Memórias", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Conteúdo Privado por Padrão: Não existem feeds públicos, perfis expostos nem contagem de seguidores.", fontSize = 12.sp)
                    Text("• Armazenamento Seguro: Suas mídias ficam salvas de forma segura na memória do aparelho com suporte a backup na nuvem.", fontSize = 12.sp)
                    Text("• Sincronização Flexível: Quando você está sem internet, o aplicativo guarda tudo no aparelho e sincroniza assim que reconectar.", fontSize = 12.sp)
                    Text("• Controle do Usuário: Você pode restaurar ou excluir permanentemente suas memórias a qualquer momento através da lixeira.", fontSize = 12.sp)
                    Text("• Transparência de IA: Recursos de inteligência são processados com seu consentimento explícito.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Entendi") }
            }
        )
    }

    // Pending Restore Confirmation Dialog
    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restaurar e Mesclar Backup", fontWeight = FontWeight.Bold) },
            text = {
                Text("Deseja importar e mesclar as memórias, pessoas, momentos e mídias contidas no arquivo selecionado? Seus dados atuais não serão apagados.", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri?.let { uri -> viewModel.restoreBackup(uri) }
                        pendingRestoreUri = null
                    }
                ) {
                    Text("Mesclar Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Menu de Preferências do Usuário
    if (showPreferencesMenu) {
        UserPreferencesMenuSheet(
            onDismissRequest = { showPreferencesMenu = false },
            onOpenAudioRecorder = {
                showPreferencesMenu = false
                showAudioRecorderModal = true
            },
            onOpenAudioMemoriesVault = {
                showPreferencesMenu = false
                showAudioMemoriesVault = true
            }
        )
    }

    // Modal de Gravação de Áudio com Microfone
    if (showAudioRecorderModal) {
        AudioRecordingBottomSheet(
            onDismissRequest = { showAudioRecorderModal = false },
            onAudioRecorded = { recordedAudio ->
                Toast.makeText(context, "Áudio gravado e salvo com sucesso: ${recordedAudio.name} 🎙️", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Modal do Cofre de Memórias de Áudio & Transcrições (Room)
    if (showAudioMemoriesVault) {
        AudioMemoriesModalSheet(
            onDismissRequest = { showAudioMemoriesVault = false }
        )
    }
}
