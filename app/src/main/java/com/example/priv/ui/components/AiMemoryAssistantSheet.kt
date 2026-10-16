package com.example.priv.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.priv.ui.viewmodel.AiAssistantUiState
import com.example.priv.ui.viewmodel.AiAssistantViewModel
import com.example.priv.ui.viewmodel.InterviewQuestionsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMemoryAssistantSheet(
    viewModel: AiAssistantViewModel,
    availableMemoriesSummary: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val interviewState by viewModel.interviewState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Perguntar ao Cofre, 1: Roteiro Familiar
    var questionInput by remember { mutableStateOf("") }
    var relativeName by remember { mutableStateOf("") }
    var relativeRole by remember { mutableStateOf("Avó / Avô") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier.testTag("ai_memory_assistant_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Gemini Branding & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Gemini AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Guardião IA",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "GEMINI 3.8",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Preservação inteligente e reflexões de memória",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Perguntar ao Cofre", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_ask_vault")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Roteiro de Voz", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_interview_guide")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // TAB 0: Perguntar ao Cofre
                Text(
                    text = "Consulte lembranças, encontre histórias ou peça reflexões:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    placeholder = { Text("Ex: O que gravamos sobre a casa da infância?", fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_gemini_question"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (questionInput.isNotBlank()) {
                            IconButton(onClick = { questionInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Suggestion chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { questionInput = "Quais as principais lições de vida preservadas nas gravações?" },
                        label = { Text("Lições de vida", fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { questionInput = "Resuma em uma mensagem afetuosa as lembranças deste mês" },
                        label = { Text("Resumo afetuoso", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (questionInput.isNotBlank()) {
                            viewModel.askMemoryVault(questionInput, availableMemoriesSummary)
                        }
                    },
                    enabled = questionInput.isNotBlank() && uiState !is AiAssistantUiState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_submit_gemini_question")
                ) {
                    if (uiState is AiAssistantUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Consultando Gemini...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Perguntar com Gemini", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Output Display
                when (val state = uiState) {
                    is AiAssistantUiState.Idle -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "A inteligência artificial analisa o histórico de memórias e vozes com total respeito à privacidade e sensibilidade afetiva.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    is AiAssistantUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Iniciando streaming com Gemini 3.8 Flash...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    is AiAssistantUiState.Streaming -> {
                        ResultCard(
                            title = "Reflexão em Andamento...",
                            content = state.text,
                            isStreaming = true,
                            onCopy = { copyToClipboard(context, state.text) }
                        )
                    }
                    is AiAssistantUiState.Success -> {
                        ResultCard(
                            title = "Resposta do Guardião",
                            content = state.text,
                            isStreaming = false,
                            onCopy = { copyToClipboard(context, state.text) }
                        )
                    }
                    is AiAssistantUiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Falha na resposta do Gemini",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = state.message,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 1: Roteiro Familiar Estruturado
                Text(
                    text = "Gere perguntas profundas para gravar e guardar a voz de quem você ama antes que seja tarde:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = relativeName,
                    onValueChange = { relativeName = it },
                    label = { Text("Nome do Ente Querido") },
                    placeholder = { Text("Ex: Vovó Maria, Meu Pai José") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_relative_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = relativeRole,
                    onValueChange = { relativeRole = it },
                    label = { Text("Relação / Vínculo Afetivo") },
                    placeholder = { Text("Ex: Mãe, Avô, Padrinho, Amigo de infância") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_relative_role")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (relativeName.isNotBlank()) {
                            viewModel.generateInterviewQuestions(relativeName, relativeRole)
                        }
                    },
                    enabled = relativeName.isNotBlank() && interviewState !is InterviewQuestionsUiState.Loading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_generate_interview_questions")
                ) {
                    if (interviewState is InterviewQuestionsUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Criando perguntas estruturadas...")
                    } else {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerar Roteiro de Gravação", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = interviewState) {
                    is InterviewQuestionsUiState.Idle -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "O Gemini estruturará 4 perguntas emotivas sob medida para guiar a gravação de voz da pessoa escolhida.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    is InterviewQuestionsUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is InterviewQuestionsUiState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.questions.forEachIndexed { index, q ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "#${index + 1} • ${q.category.uppercase()}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { copyToClipboard(context, q.question) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "Copiar pergunta",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "\"${q.question}\"",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (q.contextReason.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = q.contextReason,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is InterviewQuestionsUiState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Erro: ${state.message}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    title: String,
    content: String,
    isStreaming: Boolean,
    onCopy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isStreaming) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar resposta",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = content,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("GuardDay Gemini", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copiado para a área de transferência ✨", Toast.LENGTH_SHORT).show()
}
