package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.ui.components.GuardDayTopBar
import com.example.priv.ui.components.QuickCaptureSheet
import com.example.priv.ui.screens.*
import com.example.priv.ui.viewmodel.*
import com.example.ui.theme.PrivTheme

enum class PrivScreen {
    HOME, PERSONS_GROUPS, LIBRARY, TIMELINE_SCRAPBOOK, MOMENTS_COLLECTIONS, PROFILE
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val personViewModel: PersonViewModel by viewModels()
    private val groupsViewModel: GroupsViewModel by viewModels()
    private val momentsViewModel: MomentsAndCollectionsViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val timelineScrapbookViewModel: TimelineScrapbookViewModel by viewModels()
    private val quickCaptureViewModel: QuickCaptureViewModel by viewModels()
    private val profileViewModel: com.example.priv.data.viewmodel.ProfileViewModel by viewModels()

    private val sharedAttachments = mutableStateListOf<com.example.priv.data.model.AttachmentInput>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.example.priv.data.security.SecurityLockManager.init(this)

        handleIncomingIntent(intent)

        setContent {
            val userPrefsManager = remember { com.example.priv.data.preferences.UserPreferencesManager.getInstance(this@MainActivity) }
            val userPrefs by userPrefsManager.preferences.collectAsState()
            val isDarkTheme = when (userPrefs.themeMode) {
                com.example.priv.data.preferences.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
                com.example.priv.data.preferences.ThemeMode.LIGHT -> false
                com.example.priv.data.preferences.ThemeMode.DARK -> true
            }

            PrivTheme(
                darkTheme = isDarkTheme,
                dynamicColor = userPrefs.useDynamicColors
            ) {
                val prefs = getSharedPreferences("priv_prefs", MODE_PRIVATE)
                var hasCompletedOnboarding by remember {
                    mutableStateOf(prefs.getBoolean("has_completed_onboarding", true))
                }

                if (!hasCompletedOnboarding) {
                    com.example.priv.ui.screens.OnboardingScreen(
                        onCompleteOnboarding = { name, isGoogle ->
                            if (name.isNotBlank()) {
                                prefs.edit().putString("user_name", name).apply()
                                profileViewModel.updateProfileNameAndPhoto(name, null)
                            }
                            prefs.edit().putBoolean("has_completed_onboarding", true).apply()
                            hasCompletedOnboarding = true
                        }
                    )
                } else {
                    PrivAppContent()
                }
            }
        }
    }

    @Composable
    private fun PrivAppContent() {
        val isAppLocked by com.example.priv.data.security.SecurityLockManager.isAppLocked.collectAsState()

        if (isAppLocked) {
            com.example.priv.ui.components.AppLockOverlay(
                onUnlocked = {}
            )
            return
        }

        var currentScreen by remember { mutableStateOf(PrivScreen.HOME) }
        var showQuickCaptureSheet by remember { mutableStateOf(false) }
        var showPreferencesMenu by remember { mutableStateOf(false) }
        var showAudioVaultSheet by remember { mutableStateOf(false) }

        val userPrefsManager = remember { com.example.priv.data.preferences.UserPreferencesManager.getInstance(this@MainActivity) }
        val userPrefs by userPrefsManager.preferences.collectAsState()

        val persons by quickCaptureViewModel.persons.collectAsState()
        val groups by quickCaptureViewModel.groups.collectAsState()
        val moments by quickCaptureViewModel.moments.collectAsState()
        val collections by quickCaptureViewModel.collections.collectAsState()

        val playbackState by com.example.priv.ui.audio.AudioPlayerManager.playbackState.collectAsState()
        var showExpandedPlayerSheet by remember { mutableStateOf(false) }

        // Back navigation handling
        BackHandler(enabled = currentScreen != PrivScreen.HOME) {
            currentScreen = PrivScreen.HOME
        }

        // Auto open sheet if shared intent arrived
        LaunchedEffect(sharedAttachments.size) {
            if (sharedAttachments.isNotEmpty()) {
                showQuickCaptureSheet = true
            }
        }

        val playingPerson = persons.firstOrNull() // Default active person context for playback bar if available

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                GuardDayTopBar(
                    currentScreen = currentScreen,
                    userName = userPrefs.userName,
                    userAvatarUrl = userPrefs.userAvatarUrl,
                    onOpenUserMenu = { showPreferencesMenu = true },
                    onNavigateBack = if (currentScreen == PrivScreen.PROFILE || currentScreen == PrivScreen.TIMELINE_SCRAPBOOK || currentScreen == PrivScreen.MOMENTS_COLLECTIONS) {
                        { currentScreen = PrivScreen.HOME }
                    } else null
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    if (playbackState.memoryId != null) {
                        com.example.priv.ui.components.MiniAudioPlayerBar(
                            playbackState = playbackState,
                            title = "Tocando áudio da memória",
                            person = playingPerson,
                            onExpandPlayer = { showExpandedPlayerSheet = true }
                        )
                    }

                    NavigationBar(
                        modifier = Modifier.testTag("bottom_navigation"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == PrivScreen.HOME,
                            onClick = { currentScreen = PrivScreen.HOME },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == PrivScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Início"
                                )
                            },
                            label = { Text("Início", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_home")
                        )

                        NavigationBarItem(
                            selected = currentScreen == PrivScreen.PERSONS_GROUPS,
                            onClick = {
                                personViewModel.selectPerson(null)
                                currentScreen = PrivScreen.PERSONS_GROUPS
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == PrivScreen.PERSONS_GROUPS) Icons.Filled.People else Icons.Outlined.People,
                                    contentDescription = "Pessoas"
                                )
                            },
                            label = { Text("Pessoas", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_persons")
                        )

                        NavigationBarItem(
                            selected = currentScreen == PrivScreen.LIBRARY,
                            onClick = { currentScreen = PrivScreen.LIBRARY },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == PrivScreen.LIBRARY) Icons.Filled.GraphicEq else Icons.Outlined.GraphicEq,
                                    contentDescription = "Memórias"
                                )
                            },
                            label = { Text("Memórias", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_library")
                        )

                        NavigationBarItem(
                            selected = currentScreen == PrivScreen.TIMELINE_SCRAPBOOK,
                            onClick = { currentScreen = PrivScreen.TIMELINE_SCRAPBOOK },
                            icon = {
                                Icon(
                                    imageVector = if (currentScreen == PrivScreen.TIMELINE_SCRAPBOOK) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                    contentDescription = "Linha do Tempo"
                                )
                            },
                            label = { Text("Linha do Tempo", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.testTag("nav_timeline_canva")
                        )
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showQuickCaptureSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Guardar Memória", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("fab_quick_capture")
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    PrivScreen.HOME -> HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToPerson = { personId ->
                            personViewModel.selectPerson(personId)
                            currentScreen = PrivScreen.PERSONS_GROUPS
                        },
                        onNavigateToLibrary = { currentScreen = PrivScreen.LIBRARY },
                        onNavigateToProfile = { currentScreen = PrivScreen.PROFILE },
                        onNavigateToMoments = { currentScreen = PrivScreen.MOMENTS_COLLECTIONS },
                        onNavigateToTimeline = { currentScreen = PrivScreen.TIMELINE_SCRAPBOOK },
                        onOpenQuickCapture = { showQuickCaptureSheet = true },
                        onOpenPreferences = { showPreferencesMenu = true },
                        onOpenAudioVault = { showAudioVaultSheet = true }
                    )

                    PrivScreen.PERSONS_GROUPS -> PersonsAndGroupsScreen(
                        personViewModel = personViewModel,
                        groupsViewModel = groupsViewModel
                    )

                    PrivScreen.LIBRARY -> LibraryScreen(
                        viewModel = libraryViewModel,
                        onNavigateToPerson = { personId ->
                            personViewModel.selectPerson(personId)
                            currentScreen = PrivScreen.PERSONS_GROUPS
                        },
                        onNavigateToMoments = { currentScreen = PrivScreen.MOMENTS_COLLECTIONS }
                    )

                    PrivScreen.TIMELINE_SCRAPBOOK -> TimelineScrapbookScreen(
                        viewModel = timelineScrapbookViewModel,
                        onNavigateBack = { currentScreen = PrivScreen.HOME }
                    )

                    PrivScreen.MOMENTS_COLLECTIONS -> MomentsAndCollectionsScreen(
                        viewModel = momentsViewModel
                    )

                    PrivScreen.PROFILE -> ProfileScreen()
                }
            }

            if (showQuickCaptureSheet) {
                val activeMemoriesList by quickCaptureViewModel.activeMemories.collectAsState()

                QuickCaptureSheet(
                    persons = persons,
                    groups = groups,
                    moments = moments,
                    collections = collections,
                    existingMemories = activeMemoriesList,
                    initialAttachments = sharedAttachments.toList(),
                    onDismiss = {
                        showQuickCaptureSheet = false
                        sharedAttachments.clear()
                    },
                    onCreatePersonInline = { name, rel, onCreated ->
                        quickCaptureViewModel.createPersonInline(name, rel, onCreated = onCreated)
                    },
                    onCreateGroupInline = { name, desc, onCreated ->
                        quickCaptureViewModel.createGroupInline(name, desc, onCreated = onCreated)
                    },
                    onCreateMomentInline = { title, desc, date, onCreated ->
                        quickCaptureViewModel.createMomentInline(title, desc, date, onCreated = onCreated)
                    },
                    onCreateCollectionInline = { title, desc, onCreated ->
                        quickCaptureViewModel.createCollectionInline(title, desc, onCreated = onCreated)
                    },
                    onSaveNew = { title, note, attachments, personIds, groupIds, momentIds, collectionIds, tags ->
                        quickCaptureViewModel.saveMultimediaMemory(
                            title = title,
                            note = note,
                            attachments = attachments,
                            personIds = personIds,
                            groupIds = groupIds,
                            momentIds = momentIds,
                            collectionIds = collectionIds,
                            tagsString = tags,
                            source = if (sharedAttachments.isNotEmpty()) "Compartilhado" else "Manual",
                            onSuccess = {
                                showQuickCaptureSheet = false
                                sharedAttachments.clear()
                                Toast.makeText(this@MainActivity, "Memória guardada no GuardDay com sucesso! ✨", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onAppendToExisting = { targetId, attachments, additionalNote ->
                        quickCaptureViewModel.appendToExistingMemory(
                            targetMemoryId = targetId,
                            attachments = attachments,
                            additionalNote = additionalNote,
                            onSuccess = {
                                showQuickCaptureSheet = false
                                sharedAttachments.clear()
                                Toast.makeText(this@MainActivity, "Arquivos vinculados à memória com sucesso! 🔗", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }

            if (showExpandedPlayerSheet && playbackState.memoryId != null) {
                com.example.priv.ui.components.ExpandedAudioPlayerSheet(
                    playbackState = playbackState,
                    title = "Tocando áudio da memória",
                    person = playingPerson,
                    waveformData = playbackState.waveformData,
                    onDismiss = { showExpandedPlayerSheet = false }
                )
            }

            if (showPreferencesMenu) {
                com.example.priv.ui.preferences.UserPreferencesMenuSheet(
                    onDismissRequest = { showPreferencesMenu = false },
                    onOpenAudioMemoriesVault = {
                        showPreferencesMenu = false
                        showAudioVaultSheet = true
                    },
                    onOpenFullProfile = {
                        showPreferencesMenu = false
                        currentScreen = PrivScreen.PROFILE
                    }
                )
            }

            if (showAudioVaultSheet) {
                com.example.priv.ui.audio.AudioMemoriesModalSheet(
                    onDismissRequest = { showAudioVaultSheet = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val mimeType = intent.type ?: "*/*"

        if (action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)

            sharedAttachments.clear()
            if (uri != null) {
                val mediaType = when {
                    mimeType.startsWith("audio/") -> "AUDIO"
                    mimeType.startsWith("image/") -> "IMAGE"
                    mimeType.startsWith("video/") -> "VIDEO"
                    else -> "AUDIO"
                }
                val meta = com.example.priv.data.util.MediaMetadataExtractor.extractMetadata(this, uri)
                sharedAttachments.add(
                    com.example.priv.data.model.AttachmentInput(
                        type = mediaType,
                        uri = uri.toString(),
                        fileName = meta.fileName ?: "",
                        fileSize = meta.fileSize ?: 0L,
                        mimeType = meta.mimeType ?: mimeType,
                        width = meta.width,
                        height = meta.height,
                        capturedTimestamp = meta.capturedTimestamp,
                        durationMs = if (mediaType == "AUDIO") 30000L else 0L
                    )
                )
            }
            if (!text.isNullGlanceable()) {
                sharedAttachments.add(
                    com.example.priv.data.model.AttachmentInput(
                        type = "TEXT",
                        textContent = text ?: ""
                    )
                )
            }
            if (sharedAttachments.isNotEmpty()) {
                Toast.makeText(this, "Conteúdo recebido no GuardDay!", Toast.LENGTH_SHORT).show()
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            sharedAttachments.clear()
            uris?.forEach { uri ->
                val mediaType = when {
                    mimeType.startsWith("image/") -> "IMAGE"
                    mimeType.startsWith("video/") -> "VIDEO"
                    mimeType.startsWith("audio/") -> "AUDIO"
                    else -> "IMAGE"
                }
                val meta = com.example.priv.data.util.MediaMetadataExtractor.extractMetadata(this, uri)
                sharedAttachments.add(
                    com.example.priv.data.model.AttachmentInput(
                        type = mediaType,
                        uri = uri.toString(),
                        fileName = meta.fileName ?: "",
                        fileSize = meta.fileSize ?: 0L,
                        mimeType = meta.mimeType ?: mimeType,
                        width = meta.width,
                        height = meta.height,
                        capturedTimestamp = meta.capturedTimestamp
                    )
                )
            }
            if (sharedAttachments.isNotEmpty()) {
                Toast.makeText(this, "${sharedAttachments.size} itens recebidos no GuardDay!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun String?.isNullGlanceable(): Boolean = this.isNullOrBlank()
}
