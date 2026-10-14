package com.example.priv.ui.preferences

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.priv.data.preferences.*
import com.example.priv.util.audio.AudioRecorderManager

private enum class PrefCategory(val title: String, val icon: ImageVector) {
    PROFILE("Perfil", Icons.Default.Person),
    THEME("Aparência", Icons.Default.Palette),
    AUDIO("Áudio & Voz", Icons.Default.Mic),
    SECURITY("Privacidade", Icons.Default.Lock),
    NOTIFICATIONS("Alertas", Icons.Default.Notifications),
    STORAGE("Armazenamento", Icons.Default.Storage)
}

/**
 * Modal Bottom Sheet completo para edição e personalização das preferências do usuário no GuardDay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPreferencesMenuSheet(
    onDismissRequest: () -> Unit,
    onOpenAudioRecorder: (() -> Unit)? = null,
    onOpenAudioMemoriesVault: (() -> Unit)? = null,
    onOpenFullProfile: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefsManager = remember { UserPreferencesManager.getInstance(context) }
    val audioRecorderManager = remember { AudioRecorderManager(context) }
    val preferences by prefsManager.preferences.collectAsState()

    var selectedCategory by remember { mutableStateOf(PrefCategory.THEME) }

    // Profile state
    var editName by remember(preferences.userName) { mutableStateOf(preferences.userName) }
    var editBio by remember(preferences.userBio) { mutableStateOf(preferences.userBio) }
    var editAvatarUrl by remember(preferences.userAvatarUrl) { mutableStateOf(preferences.userAvatarUrl) }
    var editEmail by remember(preferences.userEmail) { mutableStateOf(preferences.userEmail) }

    // Audio storage cache display
    var audioStorageBytes by remember { mutableStateOf(audioRecorderManager.getTotalStorageSizeBytes()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { editAvatarUrl = it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        },
        modifier = modifier.testTag("user_preferences_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Preferências do GuardDay",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Personalize tema, perfil, áudio e segurança",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("btn_close_preferences")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Tab Selector
            ScrollableTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                PrefCategory.values().forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Tab(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Content Body
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedCategory) {
                    PrefCategory.PROFILE -> {
                        item {
                            ProfileEditSection(
                                name = editName,
                                onNameChange = { editName = it },
                                bio = editBio,
                                onBioChange = { editBio = it },
                                email = editEmail,
                                onEmailChange = { editEmail = it },
                                avatarUrl = editAvatarUrl,
                                onPickAvatar = { photoPickerLauncher.launch("image/*") },
                                onClearAvatar = { editAvatarUrl = "" },
                                onSave = {
                                    prefsManager.updateProfile(editName, editBio, editAvatarUrl, editEmail)
                                    Toast.makeText(context, "Perfil atualizado com sucesso! ✨", Toast.LENGTH_SHORT).show()
                                }
                            )
                            if (onOpenFullProfile != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = onOpenFullProfile,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_sheet_open_full_profile"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Abrir Tela Completa de Perfil & Backup", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    PrefCategory.THEME -> {
                        item {
                            ThemeSettingsSection(
                                preferences = preferences,
                                onThemeModeChange = { mode ->
                                    prefsManager.updateTheme(mode, preferences.useDynamicColors, preferences.amoledPureBlack)
                                },
                                onDynamicColorsChange = { enabled ->
                                    prefsManager.updateTheme(preferences.themeMode, enabled, preferences.amoledPureBlack)
                                },
                                onAmoledBlackChange = { enabled ->
                                    prefsManager.updateTheme(preferences.themeMode, preferences.useDynamicColors, enabled)
                                },
                                onLayoutChange = { layout ->
                                    prefsManager.updateDisplayLayout(layout)
                                }
                            )
                        }
                    }

                    PrefCategory.AUDIO -> {
                        item {
                            AudioSettingsSection(
                                preferences = preferences,
                                audioStorageBytes = audioStorageBytes,
                                onQualityChange = { quality ->
                                    prefsManager.updateAudioSettings(
                                        quality,
                                        preferences.autoTranscribeWithAi,
                                        preferences.showWaveformPreview,
                                        preferences.defaultPlaybackSpeed,
                                        preferences.saveAudioCopiesLocally
                                    )
                                },
                                onAutoTranscribeChange = { enabled ->
                                    prefsManager.updateAudioSettings(
                                        preferences.audioQuality,
                                        enabled,
                                        preferences.showWaveformPreview,
                                        preferences.defaultPlaybackSpeed,
                                        preferences.saveAudioCopiesLocally
                                    )
                                },
                                onWaveformPreviewChange = { enabled ->
                                    prefsManager.updateAudioSettings(
                                        preferences.audioQuality,
                                        preferences.autoTranscribeWithAi,
                                        enabled,
                                        preferences.defaultPlaybackSpeed,
                                        preferences.saveAudioCopiesLocally
                                    )
                                },
                                onPlaybackSpeedChange = { speed ->
                                    prefsManager.updateAudioSettings(
                                        preferences.audioQuality,
                                        preferences.autoTranscribeWithAi,
                                        preferences.showWaveformPreview,
                                        speed,
                                        preferences.saveAudioCopiesLocally
                                    )
                                },
                                onSaveCopiesChange = { enabled ->
                                    prefsManager.updateAudioSettings(
                                        preferences.audioQuality,
                                        preferences.autoTranscribeWithAi,
                                        preferences.showWaveformPreview,
                                        preferences.defaultPlaybackSpeed,
                                        enabled
                                    )
                                },
                                onClearTempCache = {
                                    val count = audioRecorderManager.clearTemporaryCache()
                                    audioStorageBytes = audioRecorderManager.getTotalStorageSizeBytes()
                                    Toast.makeText(context, "$count arquivos temporários limpos! 🧹", Toast.LENGTH_SHORT).show()
                                },
                                onTestMicRecording = onOpenAudioRecorder,
                                onOpenAudioVault = onOpenAudioMemoriesVault
                            )
                        }
                    }

                    PrefCategory.SECURITY -> {
                        item {
                            SecuritySettingsSection(
                                preferences = preferences,
                                onAutoLockChange = { timeout ->
                                    prefsManager.updateSecuritySettings(
                                        timeout,
                                        preferences.maskContentInRecentApps,
                                        preferences.discreetNotifications,
                                        preferences.contactsIntegrationEnabled
                                    )
                                },
                                onMaskRecentAppsChange = { enabled ->
                                    prefsManager.updateSecuritySettings(
                                        preferences.autoLockTimeout,
                                        enabled,
                                        preferences.discreetNotifications,
                                        preferences.contactsIntegrationEnabled
                                    )
                                },
                                onDiscreetNotificationsChange = { enabled ->
                                    prefsManager.updateSecuritySettings(
                                        preferences.autoLockTimeout,
                                        preferences.maskContentInRecentApps,
                                        enabled,
                                        preferences.contactsIntegrationEnabled
                                    )
                                },
                                onContactsEnabledChange = { enabled ->
                                    prefsManager.updateSecuritySettings(
                                        preferences.autoLockTimeout,
                                        preferences.maskContentInRecentApps,
                                        preferences.discreetNotifications,
                                        enabled
                                    )
                                }
                            )
                        }
                    }

                    PrefCategory.NOTIFICATIONS -> {
                        item {
                            NotificationsSettingsSection(
                                preferences = preferences,
                                onCapsuleAlertsChange = { enabled ->
                                    prefsManager.updateNotificationSettings(
                                        enabled,
                                        preferences.dailyMemoryReminder,
                                        preferences.reminderHour,
                                        preferences.reminderMinute
                                    )
                                },
                                onDailyReminderChange = { enabled ->
                                    prefsManager.updateNotificationSettings(
                                        preferences.capsuleUnlockNotifications,
                                        enabled,
                                        preferences.reminderHour,
                                        preferences.reminderMinute
                                    )
                                },
                                onPickTime = {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            prefsManager.updateNotificationSettings(
                                                preferences.capsuleUnlockNotifications,
                                                preferences.dailyMemoryReminder,
                                                hour,
                                                minute
                                            )
                                        },
                                        preferences.reminderHour,
                                        preferences.reminderMinute,
                                        true
                                    ).show()
                                }
                            )
                        }
                    }

                    PrefCategory.STORAGE -> {
                        item {
                            StorageSettingsSection(
                                preferences = preferences,
                                onAutoCleanChange = { enabled ->
                                    prefsManager.updateStorageSettings(enabled, preferences.confirmBeforeDelete)
                                },
                                onConfirmDeleteChange = { enabled ->
                                    prefsManager.updateStorageSettings(preferences.autoCleanTempAudioCache, enabled)
                                },
                                onResetDefaults = {
                                    prefsManager.resetToDefaults()
                                    Toast.makeText(context, "Preferências restauradas para os padrões originais.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// =========================================================================
// SEÇÃO: PERFIL DO USUÁRIO
// =========================================================================
@Composable
private fun ProfileEditSection(
    name: String,
    onNameChange: (String) -> Unit,
    bio: String,
    onBioChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    avatarUrl: String,
    onPickAvatar: () -> Unit,
    onClearAvatar: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Meu Perfil & Identidade", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Avatar Preview and Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onPickAvatar() }
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    OutlinedButton(
                        onClick = onPickAvatar,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escolher Foto", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (avatarUrl.isNotBlank()) {
                        TextButton(onClick = onClearAvatar) {
                            Text("Remover Foto", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nome de exibição") },
                placeholder = { Text("Ex: Lucas ou Heloísa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pref_input_name"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = bio,
                onValueChange = onBioChange,
                label = { Text("Frase / Lema pessoal") },
                placeholder = { Text("Ex: O amor mora nos pequenos detalhes") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("pref_input_bio"),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("E-mail para backup (opcional)") },
                placeholder = { Text("seu.email@exemplo.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pref_input_email"),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().testTag("btn_save_profile_prefs"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salvar Informações do Perfil", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================================
// SEÇÃO: TEMA & VISUAL
// =========================================================================
@Composable
private fun ThemeSettingsSection(
    preferences: UserPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onAmoledBlackChange: (Boolean) -> Unit,
    onLayoutChange: (MemoryDisplayLayout) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Tema do GuardDay", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Segmented Theme Mode Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.values().forEach { mode ->
                    val isSelected = preferences.themeMode == mode
                    val icon = when (mode) {
                        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(mode.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Dynamic Colors switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cores Dinâmicas (Material You)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Harmoniza com a paleta do papel de parede do Android 12+", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.useDynamicColors,
                    onCheckedChange = onDynamicColorsChange,
                    modifier = Modifier.testTag("switch_dynamic_colors")
                )
            }

            // AMOLED Pure Black
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Preto Puro AMOLED", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Fundo 100% escuro para máxima economia de bateria", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.amoledPureBlack,
                    onCheckedChange = onAmoledBlackChange,
                    modifier = Modifier.testTag("switch_amoled_black")
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Memory Display Layout
            Text("Layout Padrão das Memórias", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemoryDisplayLayout.values().forEach { layout ->
                    val isSelected = preferences.memoryDisplayLayout == layout
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLayoutChange(layout) },
                        label = { Text(layout.title, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// SEÇÃO: ÁUDIO & GRAVAÇÃO DE VOZ
// =========================================================================
@Composable
private fun AudioSettingsSection(
    preferences: UserPreferences,
    audioStorageBytes: Long,
    onQualityChange: (AudioRecordingQuality) -> Unit,
    onAutoTranscribeChange: (Boolean) -> Unit,
    onWaveformPreviewChange: (Boolean) -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onSaveCopiesChange: (Boolean) -> Unit,
    onClearTempCache: () -> Unit,
    onTestMicRecording: (() -> Unit)?,
    onOpenAudioVault: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gravação & Gerenciamento de Áudios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (onTestMicRecording != null) {
                    FilledTonalButton(
                        onClick = onTestMicRecording,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gravar Agora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (onOpenAudioVault != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAudioVault() }
                        .testTag("btn_pref_open_audio_vault")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    "Cofre de Memórias de Áudio & Transcrições",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Acessar banco local Room, reproduzir e editar textos",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text("Qualidade da Gravação de Microfone", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AudioRecordingQuality.values().forEach { quality ->
                    val isSelected = preferences.audioQuality == quality
                    FilterChip(
                        selected = isSelected,
                        onClick = { onQualityChange(quality) },
                        label = { Text(quality.title, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Auto Transcribe with AI Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Transcrição Automática Inteligente", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Gera resumo e texto em português ao salvar notas de voz", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.autoTranscribeWithAi,
                    onCheckedChange = onAutoTranscribeChange,
                    modifier = Modifier.testTag("switch_auto_transcribe")
                )
            }

            // Show Waveforms Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Visualizar Formas de Onda (Waveforms)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Exibe gráfico dinâmico sonoro nos cartões de áudio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.showWaveformPreview,
                    onCheckedChange = onWaveformPreviewChange,
                    modifier = Modifier.testTag("switch_show_waveform")
                )
            }

            // Default playback speed
            Column {
                Text("Velocidade Padrão do Player de Áudio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isSelected = preferences.defaultPlaybackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPlaybackSpeedChange(speed) },
                            label = { Text("${speed}x", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Save copies locally
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Salvar Cópias Seguras em Pasta Local", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Mantém os arquivos .m4a no armazenamento seguro do GuardDay", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.saveAudioCopiesLocally,
                    onCheckedChange = onSaveCopiesChange
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Storage usage & Cleaner
            val formattedSize = remember(audioStorageBytes) {
                when {
                    audioStorageBytes >= 1024 * 1024 -> String.format("%.2f MB", audioStorageBytes.toDouble() / (1024 * 1024))
                    audioStorageBytes >= 1024 -> String.format("%.1f KB", audioStorageBytes.toDouble() / 1024)
                    else -> "$audioStorageBytes bytes"
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Espaço de Áudios Salvos", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formattedSize, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedButton(
                        onClick = onClearTempCache,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpar Cache Temporário", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// =========================================================================
// SEÇÃO: PRIVACIDADE & SEGURANÇA
// =========================================================================
@Composable
private fun SecuritySettingsSection(
    preferences: UserPreferences,
    onAutoLockChange: (AutoLockTimeout) -> Unit,
    onMaskRecentAppsChange: (Boolean) -> Unit,
    onDiscreetNotificationsChange: (Boolean) -> Unit,
    onContactsEnabledChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Segurança & Cofre Privado", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Text("Tempo para Bloqueio Automático por PIN", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AutoLockTimeout.values().forEach { timeout ->
                    val isSelected = preferences.autoLockTimeout == timeout
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAutoLockChange(timeout) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onAutoLockChange(timeout) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeout.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ocultar Conteúdo na Tela de Recentes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Evita captura de tela acidental no gerenciador de tarefas do Android", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.maskContentInRecentApps,
                    onCheckedChange = onMaskRecentAppsChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notificações Discretas", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Não exibe nomes nem trechos confidenciais de memórias na barra de status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.discreetNotifications,
                    onCheckedChange = onDiscreetNotificationsChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Integração com Contatos Telefônicos", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Permite associar contatos da agenda às suas memórias no GuardDay", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.contactsIntegrationEnabled,
                    onCheckedChange = onContactsEnabledChange
                )
            }
        }
    }
}

// =========================================================================
// SEÇÃO: NOTIFICAÇÕES & CÁPSULA DO TEMPO
// =========================================================================
@Composable
private fun NotificationsSettingsSection(
    preferences: UserPreferences,
    onCapsuleAlertsChange: (Boolean) -> Unit,
    onDailyReminderChange: (Boolean) -> Unit,
    onPickTime: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Alertas & Lembretes", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Desbloqueio de Cápsulas do Tempo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Avisa quando uma cápsula futura atingir a data e hora de abertura", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.capsuleUnlockNotifications,
                    onCheckedChange = onCapsuleAlertsChange
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lembrete Diário de Memórias", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Convida você a guardar um momento ou pensamento do dia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.dailyMemoryReminder,
                    onCheckedChange = onDailyReminderChange
                )
            }

            if (preferences.dailyMemoryReminder) {
                val formattedTime = "%02d:%02d".format(preferences.reminderHour, preferences.reminderMinute)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Horário do Lembrete:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    OutlinedButton(
                        onClick = onPickTime,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(formattedTime, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// SEÇÃO: ARMAZENAMENTO & RESET
// =========================================================================
@Composable
private fun StorageSettingsSection(
    preferences: UserPreferences,
    onAutoCleanChange: (Boolean) -> Unit,
    onConfirmDeleteChange: (Boolean) -> Unit,
    onResetDefaults: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Armazenamento & Segurança dos Dados", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Limpeza Automática de Cache de Áudio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Descarta gravações canceladas ou incompletas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.autoCleanTempAudioCache,
                    onCheckedChange = onAutoCleanChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Confirmar Antes de Excluir Memórias", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Exibe diálogo preventivo para evitar exclusões acidentais", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = preferences.confirmBeforeDelete,
                    onCheckedChange = onConfirmDeleteChange
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = onResetDefaults,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restaurar Todas as Preferências para o Padrão", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
