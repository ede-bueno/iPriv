package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import com.example.priv.ui.components.QuickCaptureSheet
import com.example.priv.ui.screens.*
import com.example.priv.ui.viewmodel.*
import com.example.ui.theme.PrivTheme

enum class PrivScreen {
    HOME, PERSONS, LIBRARY, MOMENTS
}

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val personViewModel: PersonViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()
    private val quickCaptureViewModel: QuickCaptureViewModel by viewModels()

    private val sharedAudioUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            PrivTheme {
                PrivAppContent()
            }
        }
    }

    @Composable
    private fun PrivAppContent() {
        var currentScreen by remember { mutableStateOf(PrivScreen.HOME) }
        var showQuickCaptureSheet by remember { mutableStateOf(false) }

        val persons by quickCaptureViewModel.persons.collectAsState()
        val moments by quickCaptureViewModel.moments.collectAsState()
        val collections by quickCaptureViewModel.collections.collectAsState()

        val incomingUri = sharedAudioUri.value

        // Auto open sheet if shared intent arrived
        LaunchedEffect(incomingUri) {
            if (incomingUri != null) {
                showQuickCaptureSheet = true
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .testTag("bottom_navigation")
                        .windowInsetsPadding(WindowInsets.navigationBars),
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
                        label = { Text("Início", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = currentScreen == PrivScreen.PERSONS,
                        onClick = {
                            personViewModel.selectPerson(null)
                            currentScreen = PrivScreen.PERSONS
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == PrivScreen.PERSONS) Icons.Filled.People else Icons.Outlined.People,
                                contentDescription = "Pessoas"
                            )
                        },
                        label = { Text("Pessoas", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                        label = { Text("Memórias", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_library")
                    )

                    NavigationBarItem(
                        selected = currentScreen == PrivScreen.MOMENTS,
                        onClick = { currentScreen = PrivScreen.MOMENTS },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == PrivScreen.MOMENTS) Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial,
                                contentDescription = "Momentos"
                            )
                        },
                        label = { Text("Momentos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_moments")
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showQuickCaptureSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_quick_capture")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Guardar memória")
                }
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
                            currentScreen = PrivScreen.PERSONS
                        },
                        onNavigateToLibrary = { currentScreen = PrivScreen.LIBRARY },
                        onOpenQuickCapture = { showQuickCaptureSheet = true },
                        onSimulateWhatsAppShare = { title, note, durMs, personId, tags ->
                            quickCaptureViewModel.saveMemory(
                                title = title,
                                note = note,
                                audioUri = "simulated_audio_${System.currentTimeMillis()}",
                                durationMs = durMs,
                                fileSize = 420000L,
                                waveformData = "0.3,0.7,0.9,0.4,0.8,0.5,0.9,0.2,0.6,0.8,0.4",
                                transcription = "Áudio recebido via simulação rápida de compartilhamento.",
                                primaryPersonId = personId,
                                momentId = null,
                                collectionId = null,
                                tagsString = tags,
                                source = "WhatsApp",
                                onSuccess = {
                                    Toast.makeText(this@MainActivity, "Memória do WhatsApp guardada! 🎉", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )

                    PrivScreen.PERSONS -> PersonsScreen(
                        viewModel = personViewModel,
                        onSelectPerson = { personId ->
                            personViewModel.selectPerson(personId)
                        }
                    )

                    PrivScreen.LIBRARY -> LibraryScreen(
                        viewModel = libraryViewModel,
                        onNavigateToPerson = { personId ->
                            personViewModel.selectPerson(personId)
                            currentScreen = PrivScreen.PERSONS
                        }
                    )

                    PrivScreen.MOMENTS -> MomentsAndCollectionsScreen(
                        viewModel = homeViewModel
                    )
                }
            }

            if (showQuickCaptureSheet) {
                QuickCaptureSheet(
                    persons = persons,
                    moments = moments,
                    collections = collections,
                    onDismiss = {
                        showQuickCaptureSheet = false
                        sharedAudioUri.value = null
                    },
                    onSave = { title, note, personId, momentId, collectionId, tags ->
                        quickCaptureViewModel.saveMemory(
                            title = title,
                            note = note,
                            audioUri = sharedAudioUri.value?.toString() ?: "captured_audio_${System.currentTimeMillis()}",
                            durationMs = 36000L,
                            fileSize = 310000L,
                            waveformData = "0.4,0.8,0.5,0.9,0.3,0.7,0.6,0.8,0.4,0.9",
                            transcription = "",
                            primaryPersonId = personId,
                            momentId = momentId,
                            collectionId = collectionId,
                            tagsString = tags,
                            source = if (sharedAudioUri.value != null) "WhatsApp" else "Manual",
                            onSuccess = {
                                showQuickCaptureSheet = false
                                sharedAudioUri.value = null
                                Toast.makeText(this@MainActivity, "Salvou! Depois você organiza.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
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
        if (intent?.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null) {
                sharedAudioUri.value = uri
                Toast.makeText(this, "Áudio recebido no Priv!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
